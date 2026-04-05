package com.novawerk.berlinfoodmap.domain.restaurant

interface RestaurantRepository {
    suspend fun getAll(): List<Restaurant>
    suspend fun getById(id: String): Restaurant?
    suspend fun search(query: String): List<Restaurant>
    suspend fun filterByCuisine(cuisineType: CuisineType): List<Restaurant>
    suspend fun filterByDistrict(district: String): List<Restaurant>
    suspend fun filterByDistance(latitude: Double, longitude: Double, radiusKm: Double): List<Restaurant>
    suspend fun markVisited(restaurantId: String, uid: String)
    suspend fun hasVisited(restaurantId: String, uid: String): Boolean
    suspend fun incrementViewCount(restaurantId: String, uid: String)
}
