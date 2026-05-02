package com.novawerk.berlinfoodmap.ui.pages.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

@Composable
actual fun rememberLocationRequester(): LocationRequester {
    val context = LocalContext.current
    val client = remember(context) { LocationServices.getFusedLocationProviderClient(context) }

    val pendingResult = remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pendingResult.value?.invoke(granted)
        pendingResult.value = null
    }

    return remember(client, launcher) {
        AndroidLocationRequester(
            hasPermission = {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
            },
            requestPermission = { onResult ->
                pendingResult.value = onResult
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            fetchLocation = {
                val location = client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY, null,
                ).await()
                location?.let { LocationResult.Success(it.latitude, it.longitude) }
                    ?: LocationResult.Unavailable
            },
        )
    }
}

private class AndroidLocationRequester(
    private val hasPermission: () -> Boolean,
    private val requestPermission: ((Boolean) -> Unit) -> Unit,
    private val fetchLocation: suspend () -> LocationResult,
) : LocationRequester {
    override suspend fun request(): LocationResult {
        if (!hasPermission()) {
            val granted = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                requestPermission { result -> cont.resumeWith(Result.success(result)) }
            }
            if (!granted) return LocationResult.PermissionDenied
        }
        return try {
            fetchLocation()
        } catch (_: SecurityException) {
            LocationResult.PermissionDenied
        } catch (_: Exception) {
            LocationResult.Unavailable
        }
    }
}
