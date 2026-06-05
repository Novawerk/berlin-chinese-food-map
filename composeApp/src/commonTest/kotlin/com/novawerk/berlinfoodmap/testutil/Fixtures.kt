package com.novawerk.berlinfoodmap.testutil

import com.novawerk.berlinfoodmap.domain.common.Localizable
import com.novawerk.berlinfoodmap.domain.restaurant.Address
import com.novawerk.berlinfoodmap.domain.restaurant.GooglePlaceData
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.Tag

/**
 * Test data builders. Keep these minimal and explicit — every field has a
 * sensible default so a test only spells out what it actually cares about.
 */

fun localizable(en: String = "Name", zh: String = "名字", de: String? = null) =
    Localizable(en = en, zh = zh, de = de)

fun address(district: String = "Mitte", postalCode: String = "10115") =
    Address(addressLine1 = "Teststr. 1", postalCode = postalCode, district = district)

/**
 * Build a [Restaurant] for tests. [periods] (Sunday=0 minute-of-week strings, or
 * `["OPEN_24H"]`) populates a minimal [GooglePlaceData] so opening-status filters
 * can be exercised; pass null to leave googleData absent.
 */
fun restaurant(
    id: String = "r1",
    name: Localizable = localizable(),
    tags: List<Tag> = emptyList(),
    latitude: Double = 52.52,
    longitude: Double = 13.405,
    district: String = "Mitte",
    galleries: List<String> = emptyList(),
    logoUrl: String? = null,
    periods: List<String>? = null,
    coverPhotoUrl: String? = null,
    photoUrls: List<String> = emptyList(),
): Restaurant = Restaurant(
    id = id,
    name = name,
    tags = tags,
    address = address(district = district),
    latitude = latitude,
    longitude = longitude,
    galleries = galleries,
    logoUrl = logoUrl,
    googleData = if (periods != null || coverPhotoUrl != null || photoUrls.isNotEmpty()) {
        GooglePlaceData(
            placeId = "place_$id",
            periods = periods.orEmpty(),
            coverPhotoUrl = coverPhotoUrl,
            photoUrls = photoUrls,
        )
    } else {
        null
    },
)
