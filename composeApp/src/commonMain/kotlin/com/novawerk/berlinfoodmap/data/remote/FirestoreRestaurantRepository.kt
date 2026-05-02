package com.novawerk.berlinfoodmap.data.remote

import com.novawerk.berlinfoodmap.domain.common.Localizable
import com.novawerk.berlinfoodmap.domain.restaurant.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import me.tatarka.inject.annotations.Inject

@Inject
class FirestoreRestaurantRepository : RestaurantRepository {

    private val db = Firebase.firestore
    private val restaurantsRef = db.collection("restaurants")

    override suspend fun getAll(): List<Restaurant> {
        val snapshot = restaurantsRef.get()
        return snapshot.documents.map { it.toRestaurant() }.filter { !it.hidden }
    }

    override suspend fun getById(id: String): Restaurant? {
        val doc = restaurantsRef.document(id).get()
        return if (doc.exists) doc.toRestaurant() else null
    }

    override suspend fun search(query: String): List<Restaurant> {
        val all = getAll()
        val q = query.lowercase()
        return all.filter {
            it.name.en.lowercase().contains(q) ||
                it.name.zh.contains(q) ||
                (it.name.de?.lowercase()?.contains(q) == true)
        }
    }

    override suspend fun filterByCuisine(cuisineType: CuisineType): List<Restaurant> {
        val snapshot = restaurantsRef
            .where { "cuisineType" equalTo cuisineType.name }
            .get()
        return snapshot.documents.map { it.toRestaurant() }
    }

    override suspend fun filterByDistrict(district: String): List<Restaurant> {
        val snapshot = restaurantsRef
            .where { "address.district" equalTo district }
            .get()
        return snapshot.documents.map { it.toRestaurant() }
    }

    override suspend fun filterByDistance(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
    ): List<Restaurant> {
        // Client-side filtering — Firestore doesn't support geo-queries natively
        val all = getAll()
        return all.filter { restaurant ->
            haversineDistance(latitude, longitude, restaurant.latitude, restaurant.longitude) <= radiusKm
        }
    }

    override suspend fun markVisited(restaurantId: String, uid: String) {
        val visitRef = restaurantsRef.document(restaurantId)
            .collection("visits").document(uid)

        val existing = visitRef.get()
        if (!existing.exists) {
            val data: Map<String, Any> = mapOf(
                "visitedAt" to Timestamp.now().seconds,
            )
            visitRef.set(data)
            // Count is tracked via sub-collection; no direct restaurant doc update
            // to avoid PERMISSION_DENIED for anonymous users
        }
    }

    override suspend fun hasVisited(restaurantId: String, uid: String): Boolean {
        val visitRef = restaurantsRef.document(restaurantId)
            .collection("visits").document(uid)
        return visitRef.get().exists
    }

    override suspend fun incrementViewCount(restaurantId: String, uid: String) {
        val viewRef = restaurantsRef.document(restaurantId)
            .collection("views").document(uid)

        val existing = viewRef.get()
        if (existing.exists) {
            viewRef.update(
                "viewedAt" to Timestamp.now().seconds,
                "count" to FieldValue.increment(1),
            )
        } else {
            val data: Map<String, Any> = mapOf(
                "viewedAt" to Timestamp.now().seconds,
                "count" to 1,
            )
            viewRef.set(data)
        }
        // Count is tracked via sub-collection; no direct restaurant doc update
        // to avoid PERMISSION_DENIED for anonymous users
    }

    private fun haversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
    ): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(toRadians(lat1)) * kotlin.math.cos(toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    private fun toRadians(deg: Double): Double = deg * kotlin.math.PI / 180.0
}

private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toRestaurant(): Restaurant {
    val nameMap = get<Map<String, String>>("name")
    val addressMap = get<Map<String, String?>>("address")
    val descMap = try { get<Map<String, String>?>("description") } catch (_: Exception) { null }

    return Restaurant(
        id = id,
        name = Localizable(
            en = nameMap["en"] ?: "",
            zh = nameMap["zh"] ?: "",
            de = nameMap["de"],
        ),
        cuisineType = try {
            CuisineType.valueOf(get("cuisineType"))
        } catch (_: Exception) {
            CuisineType.OTHER
        },
        address = Address(
            addressLine1 = addressMap["addressLine1"] ?: "",
            addressLine2 = addressMap["addressLine2"],
            note = addressMap["note"],
            postalCode = addressMap["postalCode"] ?: "",
            district = addressMap["district"] ?: "",
            city = addressMap["city"] ?: "Berlin",
            country = addressMap["country"] ?: "Germany",
        ),
        latitude = get("latitude"),
        longitude = get("longitude"),
        phone = try { get<String?>("phone") } catch (_: Exception) { null },
        priceRange = try { get<String?>("priceRange") } catch (_: Exception) { null },
        description = descMap?.let {
            Localizable(
                en = it["en"] ?: "",
                zh = it["zh"] ?: "",
                de = it["de"],
            )
        },
        logoUrl = try { get<String?>("logoUrl") } catch (_: Exception) { null },
        galleries = try { get<List<String>>("galleries") } catch (_: Exception) { emptyList() },
        visitCount = try { get<Int>("visitCount") } catch (_: Exception) { 0 },
        viewCount = try { get<Int>("viewCount") } catch (_: Exception) { 0 },
        hidden = try { get<Boolean>("hidden") } catch (_: Exception) { false },
        googleData = try {
            get<Map<String, Any?>?>("googleData")?.toGooglePlaceData()
        } catch (_: Exception) {
            null
        },
    )
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.toGooglePlaceData(): GooglePlaceData? {
    val placeId = this["placeId"] as? String ?: return null
    val fetchedAt = this["fetchedAt"]
    return GooglePlaceData(
        placeId = placeId,
        rating = (this["rating"] as? Number)?.toDouble(),
        userRatingsTotal = (this["userRatingsTotal"] as? Number)?.toInt(),
        weekdayText = (this["weekdayText"] as? List<String>) ?: emptyList(),
        website = this["website"] as? String,
        googleMapsUrl = this["googleMapsUrl"] as? String,
        formattedPhoneNumber = this["formattedPhoneNumber"] as? String,
        formattedAddress = this["formattedAddress"] as? String,
        photoUrls = (this["photoUrls"] as? List<String>) ?: emptyList(),
        coverPhotoUrl = this["coverPhotoUrl"] as? String,
        fetchedAtEpochSeconds = when (fetchedAt) {
            is Timestamp -> fetchedAt.seconds
            is Number -> fetchedAt.toLong()
            else -> null
        },
    )
}
