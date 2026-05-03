package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.buney.maps.Marker
import eu.buney.maps.rememberUpdatedMarkerState

/**
 * Multi-restaurant cluster marker. Bitmap is keyed on count + center, so
 * it's rasterised once per cluster identity and reused for every emission.
 */
@Composable
internal fun ClusterMarker(
    cluster: RestaurantCluster,
    onClick: () -> Unit,
) {
    val state = rememberUpdatedMarkerState(position = cluster.center)
    val icon = rememberStableComposeBitmapDescriptor(cluster.items.size, cluster.center) {
        ClusterBadge(cluster.items.size)
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
