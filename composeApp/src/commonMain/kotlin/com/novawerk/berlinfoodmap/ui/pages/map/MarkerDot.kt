package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import eu.buney.maps.BitmapDescriptor
import eu.buney.maps.LatLng
import eu.buney.maps.Marker
import eu.buney.maps.rememberUpdatedMarkerState

/**
 * Visual variant the dense-marker dot takes — drives both the cache key
 * and the rendered bitmap. Only three variants exist, so the dot
 * descriptor cache is bounded at a tiny constant size for the whole
 * session.
 */
internal enum class MarkerDotKind { REGULAR, FAVORITE, FEATURED }

// All three variants render into the same DOT_BOUND_SIZE bounding box so
// the marker's anchor (0.5, 0.5) lands on the lat/lng identically — but
// the *visible* mark differs in weight. Regular is a small red dot, no
// wrapper: lots of restaurants are "just there", so they shouldn't shout.
// Favourite + featured share the larger circular badge so the user's
// saved venues and editorial picks pop against the field of small dots.
private val DOT_BOUND_SIZE = 20.dp
private val DOT_REGULAR_SIZE = 10.dp
private val DOT_INNER_HEART_SIZE = 12.dp
private val DOT_INNER_STAR_SIZE = 14.dp

// Brand-red badge fill matches the map's POI pin. White halo (1dp) keeps
// it crisp against reddish map tiles without softening the brand signal.
private val DotWrapperFill = Color(0xFFB51F1F) // PinwoRed (brand primary)
private val DotWrapperBorder = Color.White
private val DotWrapperBorderWidth = 1.dp

// Centred-symbol palette. Favourite uses white-on-red for max contrast;
// featured uses the warm brand gold so editor picks remain instantly
// recognisable as the "specially curated" marker.
private val DotInnerWhite = Color.White
private val DotInnerGold = Color(0xFFF7DD6D) // PinwoGold — brand-warm lemon

/**
 * Compact replacement for [RestaurantMarker] used when the full pill
 * would visually collide with another marker at the current zoom (see
 * [detectDenseRestaurants]). One bitmap per [MarkerDotKind] is shared
 * across every restaurant rendered as a dot via [descriptorCache] —
 * three rasterisations total per session.
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
            DotBadge(kind)
        }
        SideEffect {
            descriptorCache[kind] = rendered
        }
        rendered
    }
    Marker(
        state = state,
        icon = icon,
        // Centre anchor: the dot's geometric centre lands on the lat/lng,
        // unlike the pill which uses (0.5, 1) so its tail tip lands on it.
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
private fun DotBadge(kind: MarkerDotKind) {
    // Constant outer canvas so anchor (0.5, 0.5) lands identically across
    // variants. The visible mark sits centred inside; only favourite and
    // featured fill the canvas with the wrapper badge.
    Box(
        modifier = Modifier.size(DOT_BOUND_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        when (kind) {
            MarkerDotKind.REGULAR -> Box(
                modifier = Modifier
                    .size(DOT_REGULAR_SIZE)
                    .background(DotWrapperFill, CircleShape)
                    .border(DotWrapperBorderWidth, DotWrapperBorder, CircleShape),
            )
            MarkerDotKind.FAVORITE -> Box(
                modifier = Modifier
                    .size(DOT_BOUND_SIZE)
                    .background(DotWrapperFill, CircleShape)
                    .border(DotWrapperBorderWidth, DotWrapperBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = DotInnerWhite,
                    modifier = Modifier.size(DOT_INNER_HEART_SIZE),
                )
            }
            MarkerDotKind.FEATURED -> Box(
                modifier = Modifier
                    .size(DOT_BOUND_SIZE)
                    .background(DotWrapperFill, CircleShape)
                    .border(DotWrapperBorderWidth, DotWrapperBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = DotInnerGold,
                    modifier = Modifier.size(DOT_INNER_STAR_SIZE),
                )
            }
        }
    }
}
