package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novawerk.berlinfoodmap.di.AppScope
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.mobile
import eu.buney.maps.LatLng
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

// How recent a location fix can be before the FAB will reuse it instead
// of re-requesting. 60 s ≈ 50 m of walking drift in the worst case,
// imperceptible at the FAB's animate-to zoom of 15.
private val LOCATION_FRESHNESS = 60.seconds

/**
 * Outcome of [MapControlViewModel.ensureFreshLocation], collapsed from
 * Compass's richer sealed hierarchy into the three cases the UI actually
 * branches on.
 */
sealed interface LocationOutcome {
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
@AppScope
@Inject
class MapControlViewModel : ViewModel() {

    private val geolocator: Geolocator = Geolocator.mobile()

    var myLocation by mutableStateOf<LatLng?>(null)
        private set

    /**
     * Compass heading in degrees (0 = north). Surfaced from Compass's
     * [dev.jordond.compass.Location.azimuth] when available — drives the
     * rotation of the arrow-shaped current-location marker. Nullable
     * because the platform may not have a heading (stationary device,
     * indoors, cold start). When null the marker points north.
     */
    var bearing by mutableStateOf<Float?>(null)
        private set

    /**
     * Toggled by the locate FAB: first tap centres the camera AND turns
     * this on, drawing the 5min/15min walking-radius rings around the
     * user's location. Second tap turns it back off. Lives here (not in
     * [MapViewModel]) because it reads [myLocation].
     */
    var walkingRadiusVisible by mutableStateOf(false)
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
        // on the map by default. Skipped entirely when permission isn't
        // already granted — the user didn't ask for this attempt, so we
        // don't trigger the OS permission dialog at startup. The FAB is
        // the user-driven trigger for the first prompt.
        //
        // This path also does NOT flip [isLocating]: even when permission
        // is already granted, the underlying sensor fetch can take a few
        // seconds, and the FAB's spinner should reflect manual taps only.
        if (hasLocationPermission()) {
            viewModelScope.launch { fetchAndStore() }
        }
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
            return fetchAndStore()
        } finally {
            isLocating = false
        }
    }

    /**
     * Called by the locate FAB to flip the walking-radius rings on/off
     * each tap. The FAB also drives [ensureFreshLocation]; this method
     * is purely about the overlay visibility.
     */
    fun toggleWalkingRadius() {
        walkingRadiusVisible = !walkingRadiusVisible
    }

    private suspend fun fetchAndStore(): LocationOutcome =
        when (val result = geolocator.current()) {
            is GeolocatorResult.Success -> {
                val latLng = LatLng(
                    result.data.coordinates.latitude,
                    result.data.coordinates.longitude,
                )
                myLocation = latLng
                bearing = result.data.azimuth?.degrees?.toFloat()
                fetchedAt = TimeSource.Monotonic.markNow()
                LocationOutcome.Available(latLng)
            }
            is GeolocatorResult.PermissionError -> LocationOutcome.PermissionDenied
            is GeolocatorResult.Error -> LocationOutcome.Unavailable
        }
}
