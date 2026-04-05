package com.novawerk.berlinfoodmap.domain.restaurant

interface RestaurantRepository {
    suspend fun getAll(): List<Restaurant>
    suspend fun getById(id: String): Restaurant?
    suspend fun search(query: String): List<Restaurant>
    suspend fun filterByCuisine(cuisineType: CuisineType): List<Restaurant>
    suspend fun filterByDistrict(district: String): List<Restaurant>
}
