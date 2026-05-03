package com.novawerk.berlinfoodmap.ui.pages.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// Captured by MainActivity.onCreate before any composition runs. We hold
// the application Context (not the Activity), so this is leak-safe — it
// outlives the process, which is the lifetime we want.
@SuppressLint("StaticFieldLeak")
@Volatile
internal var locationPermissionAppContext: Context? = null

internal actual fun hasLocationPermission(): Boolean {
    val context = locationPermissionAppContext ?: return false
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    return coarse == PackageManager.PERMISSION_GRANTED ||
        fine == PackageManager.PERMISSION_GRANTED
}
