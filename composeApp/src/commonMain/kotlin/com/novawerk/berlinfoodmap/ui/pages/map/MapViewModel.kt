package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import com.novawerk.berlinfoodmap.data.store.RestaurantStore
import com.novawerk.berlinfoodmap.di.AppScope
import com.novawerk.berlinfoodmap.domain.restaurant.OpeningStatus
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.computeOpeningStatus
import eu.buney.maps.LatLngBounds
import eu.buney.maps.Projection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * UI/state holder for the map screen — filters, viewport, clustering. Pure
 * presentation logic; no IO, no Coil, no Firestore. Restaurant data and
 * cover bitmaps come from [RestaurantStore], which owns the long-running
 * observers and is process-scoped via DI.
 *
 * Compose-state-backed throughout: properties are `mutableStateOf` /
 * `derivedStateOf`, so consumers just read them in composition — Compose's
 * snapshot system handles subscriptions, no `collectAsState` boilerplate.
 *
 * Filter model: regional and format tags are independent multi-select
 * families, plus the boolean favourites / open-now toggles. Within a
 * family selections OR; across families they AND.
 */
@AppScope
@Inject
class MapViewModel(
    private val store: RestaurantStore,
) : ViewModel() {

    val allRestaurants: List<Restaurant> get() = store.restaurants
    val favorites: Set<String> get() = store.favorites
    val markerCovers: SnapshotStateMap<String, MarkerCover> get() = store.markerCovers

    var selectedCuisines by mutableStateOf<Set<Tag>>(emptySet())
        private set

    var selectedFormats by mutableStateOf<Set<Tag>>(emptySet())
        private set

    var favoritesOnly by mutableStateOf(false)
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

    val restaurantsFiltered: List<Restaurant> by derivedStateOf {
        store.restaurants.filter { r ->
            (selectedCuisines.isEmpty() || selectedCuisines.any { it in r.tags }) &&
                (selectedFormats.isEmpty() || selectedFormats.any { it in r.tags }) &&
                (!favoritesOnly || r.id in store.favorites) &&
                (!openNow || isCurrentlyServing(r))
        }
    }

    val activeFilterCount: Int by derivedStateOf {
        selectedCuisines.size +
            selectedFormats.size +
            (if (favoritesOnly) 1 else 0) +
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

    fun setCuisines(tags: Set<Tag>) {
        selectedCuisines = tags
    }

    fun setFormats(tags: Set<Tag>) {
        selectedFormats = tags
    }

    fun toggleFavoritesOnly(value: Boolean) {
        favoritesOnly = value
    }

    fun toggleOpenNow(value: Boolean) {
        openNow = value
    }

    fun resetFilters() {
        selectedCuisines = emptySet()
        selectedFormats = emptySet()
        favoritesOnly = false
        openNow = false
    }

    fun updateVisibleBounds(bounds: LatLngBounds?) {
        visibleBounds = bounds
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
