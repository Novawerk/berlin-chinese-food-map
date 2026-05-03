package com.novawerk.berlinfoodmap.ui.pages.map

import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse

internal actual fun hasLocationPermission(): Boolean {
    // `authorizationStatus` is a class method that doesn't trigger any
    // prompt — safe to call on the main thread before the first frame.
    val status = CLLocationManager.authorizationStatus()
    return status == kCLAuthorizationStatusAuthorizedAlways ||
        status == kCLAuthorizationStatusAuthorizedWhenInUse
}
