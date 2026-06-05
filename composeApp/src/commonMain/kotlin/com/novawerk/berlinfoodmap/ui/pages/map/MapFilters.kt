package com.novawerk.berlinfoodmap.ui.pages.map

import com.novawerk.berlinfoodmap.domain.restaurant.OpeningStatus
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.computeOpeningStatus
import com.novawerk.berlinfoodmap.domain.restaurant.currentBerlinMinuteOfWeek

/**
 * Pure filter logic for the map screen, extracted from [MapViewModel] so it can
 * be unit-tested without a [com.novawerk.berlinfoodmap.data.store.RestaurantStore]
 * (which owns Firestore/Coil observers). The VM's `restaurantsFiltered` is a thin
 * `derivedStateOf` wrapper around [filterRestaurants].
 *
 * Filter model (mirrors the VM doc): regional and format tags are independent
 * multi-select families. Within a family selections OR; across families they
 * AND. The favourites and open-now booleans further AND on top.
 *
 * [nowMow] is injectable purely for testing — production callers use the
 * default (current Berlin minute-of-week) so behaviour is unchanged.
 */
fun filterRestaurants(
    restaurants: List<Restaurant>,
    selectedCuisines: Set<Tag>,
    selectedFormats: Set<Tag>,
    favoritesOnly: Boolean,
    favorites: Set<String>,
    openNow: Boolean,
    nowMow: Int = currentBerlinMinuteOfWeek(),
): List<Restaurant> = restaurants.filter { r ->
    matchesFilters(r, selectedCuisines, selectedFormats, favoritesOnly, favorites, openNow, nowMow)
}

/** Single-restaurant predicate — the per-item half of [filterRestaurants]. */
fun matchesFilters(
    restaurant: Restaurant,
    selectedCuisines: Set<Tag>,
    selectedFormats: Set<Tag>,
    favoritesOnly: Boolean,
    favorites: Set<String>,
    openNow: Boolean,
    nowMow: Int = currentBerlinMinuteOfWeek(),
): Boolean =
    (selectedCuisines.isEmpty() || selectedCuisines.any { it in restaurant.tags }) &&
        (selectedFormats.isEmpty() || selectedFormats.any { it in restaurant.tags }) &&
        (!favoritesOnly || restaurant.id in favorites) &&
        (!openNow || isCurrentlyServing(restaurant, nowMow))

/**
 * True when the venue should pass the "open now" filter. Unknown hours and 24/7
 * venues pass through — we can't say with confidence they're closed, so we don't
 * filter them out; only a definite [OpeningStatus.Closed] is excluded.
 */
fun isCurrentlyServing(
    restaurant: Restaurant,
    nowMow: Int = currentBerlinMinuteOfWeek(),
): Boolean {
    val status = computeOpeningStatus(restaurant.googleData?.periods.orEmpty(), nowMow)
    return status !is OpeningStatus.Closed
}
