package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.Composable

sealed interface LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult
    data object PermissionDenied : LocationResult
    data object Unavailable : LocationResult
}

interface LocationRequester {
    suspend fun request(): LocationResult
}

@Composable
expect fun rememberLocationRequester(): LocationRequester
