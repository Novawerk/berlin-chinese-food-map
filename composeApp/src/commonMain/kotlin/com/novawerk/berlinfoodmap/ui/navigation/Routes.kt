package com.novawerk.berlinfoodmap.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object MapRoute
@Serializable data object FavoritesRoute
@Serializable data object SettingsRoute
@Serializable data class DetailRoute(val restaurantId: String)
@Serializable data class SearchRoute(
    val initialCuisine: String? = null,
    val initialDistrict: String? = null,
)
