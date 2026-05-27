package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import eu.buney.maps.BitmapDescriptor
import eu.buney.maps.LatLng
import eu.buney.maps.Marker
import eu.buney.maps.rememberUpdatedMarkerState

// Cover-thumbnail decode size for the marker pill: 32 dp rendered side × 4
// (xxxhdpi safety margin). Source photos in Firebase Storage are up to
// 1600 px wide; coil's `.size()` downsamples on decode.
internal const val MARKER_COVER_PX = 128

/**
 * Single-restaurant marker.
 *
 *  1. [cover] is supplied by the VM. `null` means "still loading" — emit
 *     nothing. Any settled value (Loaded / Failed / NoUrl) → emit.
 *  2. Check [descriptorCache] for an already-rasterised pill matching the
 *     current cover state. Cache hit → reuse. Cache miss → rasterise via
 *     `rememberStableComposeBitmapDescriptor`, publish to the cache.
 *  3. The plain `Marker` plants the bitmap on the map.
 */
@Composable
internal fun RestaurantMarker(
    restaurant: Restaurant,
    cover: MarkerCover?,
    isFavorite: Boolean,
    descriptorCache: SnapshotStateMap<String, CachedMarkerDescriptor>,
    onClick: () -> Unit,
) {
    if (cover == null) return // load still in flight

    val coverReady = cover is MarkerCover.Loaded
    val hasDiscount = restaurant.hasDiscount
    val state = rememberUpdatedMarkerState(
        position = LatLng(restaurant.latitude, restaurant.longitude),
    )

    val cached = descriptorCache[restaurant.id]
    val icon: BitmapDescriptor = if (
        cached != null &&
        cached.coverReady == coverReady &&
        cached.isFavorite == isFavorite &&
        cached.hasDiscount == hasDiscount
    ) {
        cached.descriptor
    } else {
        val rendered = rememberStableComposeBitmapDescriptor(
            restaurant.id, coverReady, isFavorite, hasDiscount,
        ) {
            MiniRestaurantCard(
                restaurant = restaurant,
                coverImage = (cover as? MarkerCover.Loaded)?.image,
                isFavorite = isFavorite,
            )
        }
        SideEffect {
            descriptorCache[restaurant.id] =
                CachedMarkerDescriptor(coverReady, isFavorite, hasDiscount, rendered)
        }
        rendered
    }

    Marker(
        state = state,
        icon = icon,
        anchor = Offset(0.5f, 1f),
        title = restaurant.name.zh,
        snippet = restaurant.name.en,
        onClick = {
            onClick()
            true
        },
    )
}
