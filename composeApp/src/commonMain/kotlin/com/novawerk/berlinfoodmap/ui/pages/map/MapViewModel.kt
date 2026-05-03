package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.previewImageUrl
import eu.buney.maps.Projection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State holder + clustering + cover-image loader for the map screen.
 *
 * Compose-state-backed throughout: properties are `mutableStateOf` /
 * `mutableStateMapOf` / `derivedStateOf`, so consumers just read them in
 * composition — Compose's snapshot system handles subscriptions, no
 * `collectAsState` boilerplate.
 *
 * Filter model: one cuisine (regional family) + one format (style
 * family), independent. Both nullable. District is NOT a filter — tapping
 * a district in the FilterSheet just pans the camera, handled in the UI
 * layer.
 *
 * Cover loading uses `imageLoader.execute(request)` directly, not
 * `AsyncImagePainter`, so it has no composition dependency and lives here
 * in `viewModelScope`. The cached `coil3.Image` is wrapped with
 * `Image.asPainter(ctx)` only at marker rasterisation time.
 */
internal class MapViewModel(
    repository: RestaurantRepository,
    private val context: PlatformContext,
) : ViewModel() {

    var allRestaurants by mutableStateOf<List<Restaurant>>(emptyList())
        private set

    var selectedCuisine by mutableStateOf<Tag?>(null)
        private set

    var selectedFormat by mutableStateOf<Tag?>(null)
        private set

    val restaurantsFiltered: List<Restaurant> by derivedStateOf {
        allRestaurants.filter { r ->
            (selectedCuisine == null || selectedCuisine in r.tags) &&
                (selectedFormat == null || selectedFormat in r.tags)
        }
    }

    val activeFilterCount: Int by derivedStateOf {
        (if (selectedCuisine != null) 1 else 0) +
            (if (selectedFormat != null) 1 else 0)
    }

    var clusters by mutableStateOf<List<RestaurantCluster>>(emptyList())
        private set

    val markerCovers = mutableStateMapOf<String, MarkerCover>()

    private val imageLoader = SingletonImageLoader.get(context)

    init {
        viewModelScope.launch {
            repository.observeAll().collect { allRestaurants = it }
        }
        viewModelScope.launch {
            // React to filter changes by reconciling the cover cache: drop
            // entries no longer in the filter, kick off loads for newcomers.
            // Existing entries are kept so cluster/zoom changes don't reload.
            snapshotFlow { restaurantsFiltered }.collect { reconcileCovers(it) }
        }
    }

    fun setCuisine(tag: Tag?) {
        selectedCuisine = tag
    }

    fun setFormat(tag: Tag?) {
        selectedFormat = tag
    }

    fun resetFilters() {
        selectedCuisine = null
        selectedFormat = null
    }

    private fun reconcileCovers(restaurants: List<Restaurant>) {
        val keep = restaurants.mapTo(HashSet()) { it.id }
        markerCovers.keys.toList().forEach { id ->
            if (id !in keep) markerCovers.remove(id)
        }

        for (r in restaurants) {
            if (r.id in markerCovers) continue
            val url = r.previewImageUrl()
            if (url == null) {
                markerCovers[r.id] = MarkerCover.NoUrl
                continue
            }
            // Each load runs in its own viewModelScope job — independent
            // lifetime from the snapshotFlow collector, so reconcile churn
            // doesn't cancel mid-load. Coil's request executor handles
            // concurrency limits internally.
            viewModelScope.launch {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(MARKER_COVER_PX, MARKER_COVER_PX)
                    .build()
                markerCovers[r.id] = when (val result = imageLoader.execute(request)) {
                    is SuccessResult -> MarkerCover.Loaded(result.image)
                    else -> MarkerCover.Failed
                }
            }
        }
    }

    /**
     * Two-phase clustering: project on Main (caller's dispatcher), grid on
     * Default. Skips state writes when the new grouping is structurally
     * identical so pan-induced no-op recomputes don't cascade into marker
     * re-emissions.
     */
    suspend fun recomputeClusters(projection: Projection, radiusPx: Float) {
        val current = restaurantsFiltered
        val projected = projectAll(current, projection)
        val next = withContext(Dispatchers.Default) {
            clusterFromProjected(current, projected, radiusPx)
        }
        if (!sameClusterGrouping(next, clusters)) {
            clusters = next
        }
    }
}
