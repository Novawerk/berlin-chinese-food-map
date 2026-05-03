package com.novawerk.berlinfoodmap.ui.pages.map

/**
 * Synchronous check for whether the OS has already granted location access.
 *
 * Used by [MapControlViewModel] to decide whether the startup-time silent
 * prefetch is allowed to call `geolocator.current()`. Without this gate, a
 * cold start on a fresh install would pop the OS permission dialog before
 * the user even sees the map — and the dialog can sit up indefinitely,
 * leaving the location request suspended.
 *
 * Returns `true` when at least one of fine/coarse (Android) or
 * when-in-use/always (iOS) is granted. Returns `false` for "not yet
 * determined" — we treat that as "don't auto-prompt; let the FAB drive
 * the first request, by user intent."
 *
 * Why a custom expect/actual instead of Compass's
 * `LocationPermissionController.hasPermission()`: Compass 1.2.2 ships a
 * sub-package `dev.jordond.compass.permissions.mobile` alongside an
 * extension function `mobile()` of the same name on
 * `LocationPermissionController.Companion`. The sub-package shadows the
 * function at the import site, so the controller can't be constructed
 * cleanly from commonMain.
 */
internal expect fun hasLocationPermission(): Boolean
