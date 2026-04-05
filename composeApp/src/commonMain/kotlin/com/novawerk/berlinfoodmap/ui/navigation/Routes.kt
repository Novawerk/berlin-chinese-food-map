package com.novawerk.berlinfoodmap.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object MapRoute
@Serializable data object ListRoute
@Serializable data class DetailRoute(val restaurantId: String)
@Serializable data object SettingsRoute
