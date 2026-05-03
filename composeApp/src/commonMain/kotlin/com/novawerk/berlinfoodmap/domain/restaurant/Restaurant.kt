package com.novawerk.berlinfoodmap.domain.restaurant

import com.novawerk.berlinfoodmap.domain.common.Localizable

data class Restaurant(
    val id: String,
    val name: Localizable,
    val tags: List<Tag>,
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
    val hidden: Boolean = false,
    val featured: Boolean = false,
    val editorialNote: Localizable? = null,
    val chain: Chain? = null,
    val googleData: GooglePlaceData? = null,
)

data class Chain(
    val brand: String,
    val branch: String? = null,
)

/**
 * Best-effort thumbnail URL for the restaurant. Admin-uploaded gallery wins
 * over Google-fetched photos so curated covers show up first.
 */
fun Restaurant.previewImageUrl(): String? =
    galleries.firstOrNull()
        ?: googleData?.coverPhotoUrl
        ?: googleData?.photoUrls?.firstOrNull()
        ?: logoUrl

/** The "primary" tag — used as a fallback display when only one chip fits. */
fun Restaurant.primaryTag(): Tag? = tags.firstOrNull()
