package com.novawerk.berlinfoodmap.domain.restaurant

/**
 * Subset of Google Places Details fetched server-side by the sync pipeline and
 * stored alongside the restaurant document. Refreshed roughly every 7 days.
 *
 * The client never calls Places directly — this keeps the app offline-capable
 * and avoids leaking user behavior to Google.
 */
data class GooglePlaceData(
    val placeId: String,
    val rating: Double? = null,
    val userRatingsTotal: Int? = null,
    val weekdayText: List<String> = emptyList(),
    /**
     * Compact "openMOW-closeMOW" strings (Sunday=0 minute-of-week) emitted by
     * the sync pipeline so the client can compute "open now" without parsing
     * locale-specific [weekdayText]. ["OPEN_24H"] is the sentinel for
     * always-open venues. Empty when Google has no hours data.
     */
    val periods: List<String> = emptyList(),
    val website: String? = null,
    val googleMapsUrl: String? = null,
    val formattedPhoneNumber: String? = null,
    val formattedAddress: String? = null,
    val photoUrls: List<String> = emptyList(),
    val coverPhotoUrl: String? = null,
    val fetchedAtEpochSeconds: Long? = null,
)
