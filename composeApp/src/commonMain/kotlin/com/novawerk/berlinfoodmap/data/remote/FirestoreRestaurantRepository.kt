package com.novawerk.berlinfoodmap.data.remote

import com.novawerk.berlinfoodmap.domain.common.Localizable
import com.novawerk.berlinfoodmap.domain.restaurant.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@Inject
class FirestoreRestaurantRepository : RestaurantRepository {

    // Firestore persistent-cache settings are applied in the native entry
    // points (MainActivity on Android, AppDelegate on iOS) BEFORE the DI
    // graph is constructed and this class touches Firestore. iOS rejects
    // settings changes via FIRIllegalStateException once the instance has
    // been "started" — and on iOS, gitlive's `Firebase.firestore` getter
    // counts as a start, so any Kotlin-side settings call here would
    // throw an NSException that runCatching can't intercept (the NSE
    // bypasses the JVM-style exception handling and crashes the process).
    // Keeping the setup native sidesteps that entirely.
    private val db = Firebase.firestore
    private val restaurantsRef = db.collection("restaurants")

    override suspend fun getAll(): List<Restaurant> {
        val snapshot = restaurantsRef.get()
        return snapshot.documents.map { it.toRestaurant() }.filter { !it.hidden }
    }

    override fun observeAll(): Flow<List<Restaurant>> =
        restaurantsRef.snapshots.map { snap ->
            snap.documents.map { it.toRestaurant() }.filter { !it.hidden }
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

    override suspend fun filterByTag(tag: Tag): List<Restaurant> {
        val snapshot = restaurantsRef
            .where { "tags" contains tag.name }
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

    val editorialMap = try { get<Map<String, String>?>("editorialNote") } catch (_: Exception) { null }
    val chainMap = try { get<Map<String, String?>?>("chain") } catch (_: Exception) { null }

    return Restaurant(
        id = id,
        name = Localizable(
            en = nameMap["en"] ?: "",
            zh = nameMap["zh"] ?: "",
            de = nameMap["de"],
        ),
        tags = try {
            get<List<String>?>("tags")
                ?.mapNotNull(::tagFromString)
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
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
        featured = try { get<Boolean>("featured") } catch (_: Exception) { false },
        hasDiscount = try { get<Boolean>("hasDiscount") } catch (_: Exception) { false },
        editorialNote = editorialMap?.takeIf { it["zh"]?.isNotBlank() == true || it["en"]?.isNotBlank() == true }?.let {
            Localizable(
                en = it["en"] ?: "",
                zh = it["zh"] ?: "",
                de = it["de"],
            )
        },
        chain = chainMap?.let { c ->
            val brand = c["brand"] ?: return@let null
            Chain(brand = brand, branch = c["branch"])
        },
        googleData = decodeGoogleData(this),
    )
}

/**
 * Decode the nested `googleData` map field by field. The naive
 * `get<Map<String, Any?>?>("googleData")` always threw under kotlinx.serialization
 * because `Any?` has no serializer — every restaurant ended up with a null
 * googleData (silently caught by a try/catch), which is why cover photos
 * never showed up in the map cards even though they were present in
 * Firestore.
 *
 * Reading each field with its concrete type (`String?`, `Double?`,
 * `List<String>?`) bypasses the issue. Native dot-notation field paths
 * are forwarded to the underlying Firestore SDK on every platform.
 */
private fun decodeGoogleData(snap: dev.gitlive.firebase.firestore.DocumentSnapshot): GooglePlaceData? {
    val placeId: String = try { snap.get<String?>("googleData.placeId") } catch (_: Exception) { null } ?: return null
    return GooglePlaceData(
        placeId = placeId,
        rating = try { snap.get<Double?>("googleData.rating") } catch (_: Exception) { null },
        userRatingsTotal = try { snap.get<Int?>("googleData.userRatingsTotal") } catch (_: Exception) { null },
        weekdayText = try { snap.get<List<String>?>("googleData.weekdayText") } catch (_: Exception) { null } ?: emptyList(),
        periods = try { snap.get<List<String>?>("googleData.periods") } catch (_: Exception) { null } ?: emptyList(),
        website = try { snap.get<String?>("googleData.website") } catch (_: Exception) { null },
        googleMapsUrl = try { snap.get<String?>("googleData.googleMapsUrl") } catch (_: Exception) { null },
        formattedPhoneNumber = try { snap.get<String?>("googleData.formattedPhoneNumber") } catch (_: Exception) { null },
        formattedAddress = try { snap.get<String?>("googleData.formattedAddress") } catch (_: Exception) { null },
        photoUrls = try { snap.get<List<String>?>("googleData.photoUrls") } catch (_: Exception) { null } ?: emptyList(),
        coverPhotoUrl = try { snap.get<String?>("googleData.coverPhotoUrl") } catch (_: Exception) { null },
        fetchedAtEpochSeconds = try { snap.get<Timestamp?>("googleData.fetchedAt")?.seconds } catch (_: Exception) { null },
    )
}
