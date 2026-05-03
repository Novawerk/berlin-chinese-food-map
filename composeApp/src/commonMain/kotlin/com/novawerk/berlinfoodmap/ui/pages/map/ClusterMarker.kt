package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.buney.maps.BitmapDescriptor
import eu.buney.maps.Marker
import eu.buney.maps.rememberUpdatedMarkerState

// Counts >= 100 all render as "99+". Clamping the cache key to this max
// keeps the cache bounded at ≤100 entries for the whole session.
private const val CLUSTER_COUNT_CLAMP = 100

/**
 * Multi-restaurant cluster marker. The bitmap depends only on the displayed
 * count, so we share a single rasterised badge across every cluster of that
 * size via [descriptorCache]. The cache survives cluster regroupings,
 * which is what kills perf on big zoom transitions.
 */
@Composable
internal fun ClusterMarker(
    cluster: RestaurantCluster,
    descriptorCache: SnapshotStateMap<Int, BitmapDescriptor>,
    onClick: () -> Unit,
) {
    val state = rememberUpdatedMarkerState(position = cluster.center)
    val displayCount = cluster.items.size.coerceAtMost(CLUSTER_COUNT_CLAMP)
    val cached = descriptorCache[displayCount]
    val icon: BitmapDescriptor = if (cached != null) {
        cached
    } else {
        val rendered = rememberStableComposeBitmapDescriptor(displayCount) {
            ClusterBadge(cluster.items.size)
        }
        SideEffect {
            descriptorCache[displayCount] = rendered
        }
        rendered
    }
    Marker(
        state = state,
        icon = icon,
        anchor = Offset(0.5f, 0.5f),
        onClick = {
            onClick()
            true
        },
    )
}

@Composable
private fun ClusterBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else "$count",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
