package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.mobile
import eu.buney.maps.LatLng
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.launch

// How recent a location fix can be before the FAB will reuse it instead
// of re-requesting. 60 s ≈ 50 m of walking drift in the worst case,
// imperceptible at the FAB's animate-to zoom of 15.
private val LOCATION_FRESHNESS = 60.seconds

/**
 * Outcome of [MapControlViewModel.ensureFreshLocation], collapsed from
 * Compass's richer sealed hierarchy into the three cases the UI actually
 * branches on.
 */
internal sealed interface LocationOutcome {
    data class Available(val coords: LatLng) : LocationOutcome
    data object PermissionDenied : LocationOutcome
    data object Unavailable : LocationOutcome
}

/**
 * Map-control state independent of the restaurant pipeline ([MapViewModel]).
 *
 * Backed by Compass's [Geolocator] for location acquisition. Compass owns
 * the platform plumbing (FusedLocationProvider on Android, CLLocationManager
 * on iOS) and handles the runtime permission flow internally — no
 * `rememberLauncherForActivityResult`, no `LocalContext`, no per-call
 * requester injection. This VM is fully self-contained.
 *
 * Why the camera state isn't here: `rememberCameraPositionState` is a
 * composable bound to the [eu.buney.maps.GoogleMap] composable's lifecycle.
 * Hoisting the state holder to a VM would either drift out of sync with
 * the live map or require a custom factory dance. The composable owns
 * `cameraPositionState` and reads decisions (e.g. [isLocationFresh]) from
 * this VM.
 */
internal class MapControlViewModel : ViewModel() {

    private val geolocator: Geolocator = Geolocator.mobile()

    var myLocation by mutableStateOf<LatLng?>(null)
        private set

    /**
     * `true` while a sensor fetch is in flight (i.e. cache miss). Stays
     * `false` for the cache-hit fast path so the FAB doesn't flash a
     * spinner on rapid re-taps.
     */
    var isLocating by mutableStateOf(false)
        private set

    // Plain (non-Compose) field: `elapsedNow()` isn't snapshot-tracked,
    // so promoting this to `mutableStateOf` would be misleading — a
    // `derivedStateOf` reading it would never re-fire as time passes.
    // Read on demand at FAB-tap time only.
    private var fetchedAt: TimeSource.Monotonic.ValueTimeMark? = null

    init {
        // Best-effort first fix when the VM is created so the blue dot is
        // on the map by default. Failures (denied / unavailable) are
        // silent — the user didn't ask for this attempt; the FAB will
        // re-prompt and surface errors when tapped.
        viewModelScope.launch { ensureFreshLocation() }
    }

    fun isLocationFresh(): Boolean =
        fetchedAt?.elapsedNow()?.let { it < LOCATION_FRESHNESS } == true

    /**
     * Cache-then-fetch orchestration. Returns the freshest known location:
     * the cached fix if still within [LOCATION_FRESHNESS], otherwise asks
     * Compass for a new one and writes it back.
     *
     * Permission prompts are handled by Compass on first call; subsequent
     * calls inherit the granted/denied state without re-prompting.
     *
     * [isLocating] flips to `true` only on the slow path (actual sensor
     * call) so callers can drive a spinner without it flashing on cache
     * hits.
     */
    suspend fun ensureFreshLocation(): LocationOutcome {
        val cached = myLocation
        if (cached != null && isLocationFresh()) {
            return LocationOutcome.Available(cached)
        }
        isLocating = true
        try {
            return when (val result = geolocator.current()) {
                is GeolocatorResult.Success -> {
                    val latLng = LatLng(
                        result.data.coordinates.latitude,
                        result.data.coordinates.longitude,
                    )
                    myLocation = latLng
                    fetchedAt = TimeSource.Monotonic.markNow()
                    LocationOutcome.Available(latLng)
                }
                is GeolocatorResult.PermissionError -> LocationOutcome.PermissionDenied
                is GeolocatorResult.Error -> LocationOutcome.Unavailable
            }
        } finally {
            isLocating = false
        }
    }
}
