package com.novawerk.berlinfoodmap.ui.pages.map

import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.testutil.restaurant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapFiltersTest {

    // Monday 10:00 — a fixed "now" so open-now assertions are deterministic.
    private val nowMon10 = 2040

    private val sichuanHotpot = restaurant(id = "sichuanHotpot", tags = listOf(Tag.SICHUAN, Tag.HOTPOT))
    private val cantonese = restaurant(id = "cantonese", tags = listOf(Tag.CANTONESE))
    private val hotpotOnly = restaurant(id = "hotpotOnly", tags = listOf(Tag.HOTPOT))
    private val bbq = restaurant(id = "bbq", tags = listOf(Tag.BBQ))

    private val all = listOf(sichuanHotpot, cantonese, hotpotOnly, bbq)

    private fun filter(
        cuisines: Set<Tag> = emptySet(),
        formats: Set<Tag> = emptySet(),
        favoritesOnly: Boolean = false,
        favorites: Set<String> = emptySet(),
        openNow: Boolean = false,
        restaurants: List<com.novawerk.berlinfoodmap.domain.restaurant.Restaurant> = all,
    ) = filterRestaurants(restaurants, cuisines, formats, favoritesOnly, favorites, openNow, nowMow = nowMon10)
        .map { it.id }
        .toSet()

    @Test
    fun noFilters_returnsEverything() {
        assertEquals(all.map { it.id }.toSet(), filter())
    }

    @Test
    fun singleCuisine_matchesThatTag() {
        assertEquals(setOf("sichuanHotpot"), filter(cuisines = setOf(Tag.SICHUAN)))
    }

    @Test
    fun multipleCuisines_orWithinFamily() {
        assertEquals(setOf("sichuanHotpot", "cantonese"), filter(cuisines = setOf(Tag.SICHUAN, Tag.CANTONESE)))
    }

    @Test
    fun singleFormat_matchesThatTag() {
        assertEquals(setOf("sichuanHotpot", "hotpotOnly"), filter(formats = setOf(Tag.HOTPOT)))
    }

    @Test
    fun cuisineAndFormat_andAcrossFamilies() {
        // SICHUAN (cuisine) AND HOTPOT (format) — only the venue carrying both.
        assertEquals(setOf("sichuanHotpot"), filter(cuisines = setOf(Tag.SICHUAN), formats = setOf(Tag.HOTPOT)))
    }

    @Test
    fun cuisineAndFormat_noOverlap_isEmpty() {
        // CANTONESE has no HOTPOT format -> AND across families yields nothing.
        assertTrue(filter(cuisines = setOf(Tag.CANTONESE), formats = setOf(Tag.HOTPOT)).isEmpty())
    }

    @Test
    fun favoritesOnly_keepsOnlyFavorited() {
        assertEquals(setOf("cantonese", "bbq"), filter(favoritesOnly = true, favorites = setOf("cantonese", "bbq")))
    }

    @Test
    fun favoritesOnly_composesWithTagFilter() {
        assertEquals(
            setOf("sichuanHotpot"),
            filter(cuisines = setOf(Tag.SICHUAN), favoritesOnly = true, favorites = setOf("sichuanHotpot", "cantonese")),
        )
    }

    @Test
    fun openNow_excludesDefinitelyClosed_keepsOpenUnknownAndAlwaysOpen() {
        val open = restaurant(id = "open", periods = listOf("1980-2460"))     // Mon 09:00-17:00
        val closed = restaurant(id = "closed", periods = listOf("1980-2010")) // Mon 09:00-09:30 (shut by 10:00)
        val unknown = restaurant(id = "unknown", periods = null)              // no hours data
        val always = restaurant(id = "always", periods = listOf("OPEN_24H"))

        val result = filter(openNow = true, restaurants = listOf(open, closed, unknown, always))
        assertEquals(setOf("open", "unknown", "always"), result)
    }

    @Test
    fun matchesFilters_singleItemPredicate() {
        assertTrue(matchesFilters(sichuanHotpot, setOf(Tag.SICHUAN), emptySet(), false, emptySet(), false, nowMon10))
        assertFalse(matchesFilters(cantonese, setOf(Tag.SICHUAN), emptySet(), false, emptySet(), false, nowMon10))
    }
}
