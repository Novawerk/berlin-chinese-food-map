package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.buney.maps.LatLng

/**
 * Cross-screen command bus for driving the map's camera and locate flow from
 * UI surfaces that live outside [MapScreen] — the main bottom nav (locate
 * tap) and the search/filter sheet (tap a result, tap a district).
 *
 * `cameraPositionState` is composable-bound to the live `GoogleMap`, so it
 * can't be hoisted into a VM and shared. Instead the holder exposes
 * state-backed "request" slots that MapScreen consumes via `LaunchedEffect`
 * and clears once handled. Locate uses a monotonic tick so repeated taps
 * still fire even when nothing else changes.
 */
@Stable
class MapCommands {
    /** Incremented each time the user taps the locate button. */
    var locateTick by mutableStateOf(0)
        private set

    /** Pending camera pan target. Set by callers, cleared by MapScreen. */
    var pendingPan by mutableStateOf<PanRequest?>(null)

    fun requestLocate() {
        locateTick += 1
    }

    fun requestPan(target: LatLng, zoom: Float) {
        pendingPan = PanRequest(target, zoom)
    }

    fun clearPan() {
        pendingPan = null
    }
}

data class PanRequest(val target: LatLng, val zoom: Float)
