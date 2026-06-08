package com.novawerk.berlinfoodmap.domain.restaurant

import kotlinx.coroutines.flow.Flow

interface RestaurantRepository {
    suspend fun getAll(): List<Restaurant>

    /**
     * Emits cached results immediately (when available) and then fresh server
     * results once they arrive. Use this on screens where first paint matters
     * more than guaranteed freshness.
     */
    fun observeAll(): Flow<List<Restaurant>>

    suspend fun getById(id: String): Restaurant?
    suspend fun search(query: String): List<Restaurant>
    suspend fun filterByTag(tag: Tag): List<Restaurant>
    suspend fun filterByDistrict(district: String): List<Restaurant>
    suspend fun filterByDistance(latitude: Double, longitude: Double, radiusKm: Double): List<Restaurant>
    suspend fun incrementViewCount(restaurantId: String, uid: String)
}
