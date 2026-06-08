package com.novawerk.berlinfoodmap.data.remote

import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure query helpers used by [FirestoreRestaurantRepository] for the client-side
 * filters Firestore can't express (free-text name search, geo radius). Extracted
 * as top-level functions so they're unit-testable without standing up Firebase —
 * the repository can't be constructed in a test because it touches
 * `Firebase.firestore` at field-init time.
 */

private const val EARTH_RADIUS_KM = 6371.0

/** Great-circle distance between two lat/lng points, in kilometres. */
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = toRadians(lat2 - lat1)
    val dLon = toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(toRadians(lat1)) * cos(toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_KM * c
}

private fun toRadians(deg: Double): Double = deg * PI / 180.0

/**
 * Whether [restaurant]'s name matches the free-text [query]. Matches the EN, ZH,
 * and DE names. EN/DE are compared case-insensitively; ZH is matched as-is since
 * Han characters have no case. A blank query matches everything.
 */
fun nameMatchesQuery(restaurant: Restaurant, query: String): Boolean {
    val q = query.lowercase()
    if (q.isBlank()) return true
    val name = restaurant.name
    return name.en.lowercase().contains(q) ||
        name.zh.contains(q) ||
        (name.de?.lowercase()?.contains(q) == true)
}
