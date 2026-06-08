package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.marker_discount
import berlinfoodmap.composeapp.generated.resources.marker_favorite
import berlinfoodmap.composeapp.generated.resources.marker_regular
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import eu.buney.maps.BitmapDescriptor
import eu.buney.maps.LatLng
import eu.buney.maps.Marker
import eu.buney.maps.rememberUpdatedMarkerState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Visual variant the dense-marker dot takes — drives both the cache key
 * and the rendered bitmap. Per the May-24 design (PDF page 5):
 *
 *  - [REGULAR]: small solid red dot. The default; lots of restaurants
 *    are just there, so they shouldn't shout.
 *  - [FAVORITE]: larger red circle with a dark-red (450100) inner —
 *    the user's saved venues pop above the field of small dots.
 *  - [DISCOUNT]: red circle with a white diamond inside — Pinwo
 *    partner discount marker.
 *  - [DISCOUNT_FAVORITE]: combined (currently drawn as the discount
 *    marker; the venue's saved state shows on the pill corner badge).
 *
 * Icons are PNG assets exported directly from the v2 Sketch design
 * file (`drawable/marker_*.png`) — no Compose primitives, so the
 * shapes exactly match the design including the subtle drop shadow.
 *
 * The old `FEATURED` (editor's pick) variant was dropped in the May-24
 * redesign — `Restaurant.featured` still exists for content curation
 * but no longer surfaces as a map marker.
 */
internal enum class MarkerDotKind { REGULAR, FAVORITE, DISCOUNT, DISCOUNT_FAVORITE }

// All variants render into the same DOT_BOUND_SIZE bounding box so the
// marker's anchor (0.5, 0.5) lands on the lat/lng identically. Special
// markers (favorite/discount) are visually larger but the bounding box
// stays the same so projection-based collision detection sees a
// consistent footprint regardless of variant.
private val DOT_BOUND_SIZE = 22.dp
private val DOT_REGULAR_SIZE = 11.dp
private val DOT_SPECIAL_SIZE = 20.dp

/**
 * Compact replacement for [RestaurantMarker] used when the full pill
 * would visually collide with another marker at the current zoom (see
 * [detectDenseRestaurants]). One bitmap per [MarkerDotKind] is shared
 * across every restaurant rendered as that variant — four
 * rasterisations total per session.
 */
@Composable
internal fun MarkerDot(
    restaurant: Restaurant,
    kind: MarkerDotKind,
    descriptorCache: SnapshotStateMap<MarkerDotKind, BitmapDescriptor>,
    onClick: () -> Unit,
) {
    val state = rememberUpdatedMarkerState(
        position = LatLng(restaurant.latitude, restaurant.longitude),
    )
    val cached = descriptorCache[kind]
    val icon: BitmapDescriptor = if (cached != null) {
        cached
    } else {
        val rendered = rememberStableComposeBitmapDescriptor(kind) {
            DotIcon(kind)
        }
        SideEffect {
            descriptorCache[kind] = rendered
        }
        rendered
    }
    Marker(
        state = state,
        icon = icon,
        anchor = Offset(0.5f, 0.5f),
        title = restaurant.name.zh,
        snippet = restaurant.name.en,
        onClick = {
            onClick()
            true
        },
    )
}

@Composable
private fun DotIcon(kind: MarkerDotKind) {
    val drawable: DrawableResource = when (kind) {
        MarkerDotKind.REGULAR -> Res.drawable.marker_regular
        MarkerDotKind.FAVORITE -> Res.drawable.marker_favorite
        MarkerDotKind.DISCOUNT, MarkerDotKind.DISCOUNT_FAVORITE -> Res.drawable.marker_discount
    }
    val size = if (kind == MarkerDotKind.REGULAR) DOT_REGULAR_SIZE else DOT_SPECIAL_SIZE
    Box(
        modifier = Modifier.size(DOT_BOUND_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    }
}
