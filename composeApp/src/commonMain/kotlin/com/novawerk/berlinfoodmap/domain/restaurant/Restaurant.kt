package com.novawerk.berlinfoodmap.domain.restaurant

import com.novawerk.berlinfoodmap.domain.common.Localizable

data class Restaurant(
    val id: String,
    val name: Localizable,
    val cuisineType: CuisineType,
    val address: Address,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val priceRange: String? = null,
    val description: Localizable? = null,
    val logoUrl: String? = null,
    val galleries: List<String> = emptyList(),
    val visitCount: Int = 0,
    val viewCount: Int = 0,
)
