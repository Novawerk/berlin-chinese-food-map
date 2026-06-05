package com.novawerk.berlinfoodmap.data.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.novawerk.berlinfoodmap.di.AppScope
import com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.domain.restaurant.previewImageUrl
import com.novawerk.berlinfoodmap.ui.pages.map.MARKER_COVER_PX
import com.novawerk.berlinfoodmap.ui.pages.map.MarkerCover
import com.novawerk.berlinfoodmap.ui.pages.map.disableHardwareBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * Process-lifetime data layer for the map: restaurants, favorites, and the
 * Coil-loaded cover bitmap cache. Owns its own [CoroutineScope] so observers
 * are independent of any ViewModel/Composable lifecycle — the moment this
 * singleton is constructed (touched at the top of `App()` during splash),
 * the Firestore + DataStore watchers fire and cover photos start loading.
 *
 * State is held as Compose snapshot state, not Flows, so consumers
 * (MapViewModel, search bar, etc.) just read the properties in composition
 * and Compose's snapshot system handles subscriptions for free.
 *
 * No filtering or UI-derived state lives here — that belongs to MapViewModel.
 */
@AppScope
@Inject
class RestaurantStore(
    restaurantRepository: RestaurantRepository,
    favoritesRepository: FavoritesRepository,
    private val context: PlatformContext,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var restaurants by mutableStateOf<List<Restaurant>>(emptyList())
        private set

    var favorites by mutableStateOf<Set<String>>(emptySet())
        private set

    /**
     * Cover-photo cache keyed by restaurant id. Filled lazily as the
     * restaurants list arrives — entries can be [MarkerCover.Loaded],
     * [MarkerCover.Failed], or [MarkerCover.NoUrl]; absent means "still
     * in flight". Map markers read this to render the cover thumbnail.
     */
    val markerCovers = mutableStateMapOf<String, MarkerCover>()

    private val imageLoader = SingletonImageLoader.get(context)

    init {
        scope.launch {
            restaurantRepository.observeAll().collect { list ->
                restaurants = list
                reconcileCovers(list)
            }
        }
        scope.launch {
            favoritesRepository.observe().collect { favorites = it }
        }
    }

    private fun reconcileCovers(list: List<Restaurant>) {
        val keep = list.mapTo(HashSet()) { it.id }
        markerCovers.retainKeys(keep)
        for (r in list) {
            if (r.id in markerCovers) continue
            val url = r.previewImageUrl()
            if (url == null) {
                markerCovers[r.id] = MarkerCover.NoUrl
                continue
            }
            // One launch per restaurant so a slow load doesn't block the rest.
            // Coil's executor handles concurrency limits internally.
            scope.launch {
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
}
