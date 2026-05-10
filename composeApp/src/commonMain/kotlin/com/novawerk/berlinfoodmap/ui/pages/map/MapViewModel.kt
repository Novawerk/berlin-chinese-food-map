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
import com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository
import com.novawerk.berlinfoodmap.domain.restaurant.OpeningStatus
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.computeOpeningStatus
import com.novawerk.berlinfoodmap.domain.restaurant.previewImageUrl
import eu.buney.maps.LatLngBounds
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
    private val favoritesRepository: FavoritesRepository,
    private val context: PlatformContext,
) : ViewModel() {

    var allRestaurants by mutableStateOf<List<Restaurant>>(emptyList())
        private set

    /**
     * Multi-select cuisine filter. Within a family, selections are OR-ed
     * (any match in the set passes); across families, AND. Empty set means
     * "no filter on this family". Same model for [selectedFormats].
     */
    var selectedCuisines by mutableStateOf<Set<Tag>>(emptySet())
        private set

    var selectedFormats by mutableStateOf<Set<Tag>>(emptySet())
        private set

    var favoritesOnly by mutableStateOf(false)
        private set

    /**
     * "Editor's Picks only" filter — restaurants with the editorial
     * `featured` flag set in the YAML data. The same flag drives the star
     * indicator on cards and markers.
     */
    var featuredOnly by mutableStateOf(false)
        private set

    /**
     * "Currently open" filter — only show restaurants whose opening
     * status is not [OpeningStatus.Closed]. Unknown / always-open
     * statuses pass so we don't penalise venues with missing hours data.
     *
     * Caveat: the filter is recomputed on Compose state changes only,
     * not on a wall-clock timer. A user who toggles the filter at 17:59
     * and lingers past 18:00 may see a venue that just closed; toggling
     * the sheet open and applying again refreshes the result.
     */
    var openNow by mutableStateOf(false)
        private set

    /** Local-favorites set, kept in sync with [FavoritesRepository]. */
    var favorites by mutableStateOf<Set<String>>(emptySet())
        private set

    val restaurantsFiltered: List<Restaurant> by derivedStateOf {
        allRestaurants.filter { r ->
            (selectedCuisines.isEmpty() || selectedCuisines.any { it in r.tags }) &&
                (selectedFormats.isEmpty() || selectedFormats.any { it in r.tags }) &&
                (!favoritesOnly || r.id in favorites) &&
                (!featuredOnly || r.featured) &&
                (!openNow || isCurrentlyServing(r))
        }
    }

    val activeFilterCount: Int by derivedStateOf {
        selectedCuisines.size +
            selectedFormats.size +
            (if (favoritesOnly) 1 else 0) +
            (if (featuredOnly) 1 else 0) +
            (if (openNow) 1 else 0)
    }

    private fun isCurrentlyServing(r: Restaurant): Boolean {
        // Unknown hours and 24/7 venues pass through — we can't say with
        // confidence they're closed, so don't filter them out.
        val status = computeOpeningStatus(r.googleData?.periods.orEmpty())
        return status !is OpeningStatus.Closed
    }

    /**
     * Restaurants whose full pill card would visually collide with at
     * least one other pill at the current zoom. The map screen renders
     * these as compact [MarkerDot] markers (dot / heart / star) instead
     * of full pills. Recomputed on filter changes (immediate) and on
     * zoom-idle (lazy — pan is invariant under our screen-distance
     * detection).
     */
    var denseIds by mutableStateOf<Set<String>>(emptySet())
        private set

    /**
     * Current map viewport. Pushed in by the composable each time
     * `cameraPositionState.projection.visibleBounds` changes; the rest of
     * the screen reads [visibleRestaurants] derived from it.
     *
     * Lives here (not in composable as a `derivedStateOf`) so MapScreen
     * stays a display layer — bounds → filtered list is data work.
     */
    var visibleBounds by mutableStateOf<LatLngBounds?>(null)
        private set

    val visibleRestaurants: List<Restaurant> by derivedStateOf {
        val bounds = visibleBounds ?: return@derivedStateOf restaurantsFiltered
        restaurantsFiltered.filter { r ->
            r.latitude in bounds.southwest.latitude..bounds.northeast.latitude &&
                r.longitude in bounds.southwest.longitude..bounds.northeast.longitude
        }
    }

    val markerCovers = mutableStateMapOf<String, MarkerCover>()

    private val imageLoader = SingletonImageLoader.get(context)

    init {
        viewModelScope.launch {
            repository.observeAll().collect { allRestaurants = it }
        }
        viewModelScope.launch {
            favoritesRepository.observe().collect { favorites = it }
        }
        viewModelScope.launch {
            // React to filter changes by reconciling the cover cache: drop
            // entries no longer in the filter, kick off loads for newcomers.
            // Existing entries are kept so cluster/zoom changes don't reload.
            snapshotFlow { restaurantsFiltered }.collect { reconcileCovers(it) }
        }
    }

    fun setCuisines(tags: Set<Tag>) {
        selectedCuisines = tags
    }

    fun setFormats(tags: Set<Tag>) {
        selectedFormats = tags
    }

    fun toggleFavoritesOnly(value: Boolean) {
        favoritesOnly = value
    }

    fun toggleFeaturedOnly(value: Boolean) {
        featuredOnly = value
    }

    fun toggleOpenNow(value: Boolean) {
        openNow = value
    }

    fun resetFilters() {
        selectedCuisines = emptySet()
        selectedFormats = emptySet()
        favoritesOnly = false
        featuredOnly = false
        openNow = false
    }

    fun updateVisibleBounds(bounds: LatLngBounds?) {
        visibleBounds = bounds
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
                    .disableHardwareBitmap()
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
     * Two-phase dense-marker detection: project on Main (caller's
     * dispatcher), grid on Default. Skips state writes when the new
     * dense set is identical so pan-induced no-op recomputes don't
     * cascade into marker re-emissions.
     *
     * [widthEstimator] returns each restaurant's pill width in px. Caller
     * computes this with `Density` in scope (composable side); the VM
     * stays platform/density-agnostic. [maxOverlapPx] must be ≥ the
     * largest possible center-to-center distance at which two pills can
     * still overlap — see [detectDenseRestaurants].
     */
    suspend fun recomputeDenseIds(
        projection: Projection,
        widthEstimator: (Restaurant) -> Float,
        heightPx: Float,
        paddingPx: Float,
        maxOverlapPx: Float,
    ) {
        val current = restaurantsFiltered
        val projected = projectAll(current, projection)
        val widths = FloatArray(current.size) { i -> widthEstimator(current[i]) }
        val next = withContext(Dispatchers.Default) {
            detectDenseRestaurants(
                restaurants = current,
                projected = projected,
                widthsPx = widths,
                heightPx = heightPx,
                paddingPx = paddingPx,
                maxOverlapPx = maxOverlapPx,
            )
        }
        if (next != denseIds) {
            denseIds = next
        }
    }
}
