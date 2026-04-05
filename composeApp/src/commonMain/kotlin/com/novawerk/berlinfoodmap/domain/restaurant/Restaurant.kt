package com.novawerk.berlinfoodmap.domain.restaurant

data class Restaurant(
    val id: String,
    val name: String,
    val nameZh: String,
    val cuisineType: CuisineType,
    val district: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val priceRange: String? = null,
    val rating: Float? = null,
    val photoUrl: String? = null,
    val openingHours: String? = null,
    val dishes: List<Dish> = emptyList(),
)
