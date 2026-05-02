package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberLocationRequester(): LocationRequester {
    val manager = remember { CLLocationManager() }
    return remember(manager) { IosLocationRequester(manager) }
}

@OptIn(ExperimentalForeignApi::class)
private class IosLocationRequester(
    private val manager: CLLocationManager,
) : LocationRequester {

    override suspend fun request(): LocationResult {
        val status = CLLocationManager.authorizationStatus()
        if (status == kCLAuthorizationStatusDenied || status == kCLAuthorizationStatusRestricted) {
            return LocationResult.PermissionDenied
        }
        if (status == kCLAuthorizationStatusNotDetermined) {
            manager.requestWhenInUseAuthorization()
            // Best-effort: continue and let the location request itself handle denial.
        }

        return suspendCancellableCoroutine { cont ->
            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager,
                    didUpdateLocations: List<*>,
                ) {
                    val location = didUpdateLocations.lastOrNull() as? CLLocation
                    val result = location?.coordinate?.useContents {
                        LocationResult.Success(latitude, longitude)
                    } ?: LocationResult.Unavailable
                    manager.delegate = null
                    if (cont.isActive) cont.resume(result)
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didFailWithError: NSError,
                ) {
                    manager.delegate = null
                    if (cont.isActive) cont.resume(LocationResult.Unavailable)
                }
            }
            manager.delegate = delegate
            manager.requestLocation()
            cont.invokeOnCancellation { manager.delegate = null }
        }
    }
}
