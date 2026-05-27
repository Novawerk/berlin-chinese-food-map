package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.nav_locate
import org.jetbrains.compose.resources.stringResource

/**
 * Floating action button anchored to the top-right of the map.
 *
 * Always-grey neutral surface per the May-24 design (PDF page 4) — the
 * FAB doesn't change appearance based on walking-radius state. The
 * rings' visibility on the map itself is the user's feedback that the
 * feature is on. A spinner overlays the icon while a sensor fetch is in
 * flight.
 *
 * Taps cycle: first tap = locate + show walking-radius rings; second
 * tap = hide rings.
 */
@Composable
internal fun LocateFab(
    isLocating: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
        modifier = modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = stringResource(Res.string.nav_locate),
                modifier = Modifier.size(22.dp),
            )
            if (isLocating) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onSurface,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}
