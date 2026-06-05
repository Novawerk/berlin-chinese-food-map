package com.novawerk.berlinfoodmap.data.remote

import com.novawerk.berlinfoodmap.testutil.localizable
import com.novawerk.berlinfoodmap.testutil.restaurant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestaurantQueryTest {

    @Test
    fun haversine_samePoint_isZero() {
        assertEquals(0.0, haversineKm(52.52, 13.405, 52.52, 13.405), 1e-9)
    }

    @Test
    fun haversine_shortEastWestHop_isUnderAKilometre() {
        // 0.01° of longitude at Berlin's latitude is ~0.68 km.
        val d = haversineKm(52.52, 13.405, 52.52, 13.415)
        assertTrue(d in 0.6..0.75, "expected ~0.68 km, got $d")
    }

    @Test
    fun haversine_berlinToMunich_isAboutFiveHundredKm() {
        val d = haversineKm(52.520, 13.405, 48.137, 11.575)
        assertTrue(d in 495.0..515.0, "expected ~504 km, got $d")
    }

    @Test
    fun haversine_isSymmetric() {
        val a = haversineKm(52.52, 13.405, 48.137, 11.575)
        val b = haversineKm(48.137, 11.575, 52.52, 13.405)
        assertEquals(a, b, 1e-9)
    }

    private val r = restaurant(name = localizable(en = "Beef Noodles", zh = "牛肉面", de = "Rindfleischnudeln"))

    @Test
    fun nameMatch_englishIsCaseInsensitive() {
        assertTrue(nameMatchesQuery(r, "beef"))
        assertTrue(nameMatchesQuery(r, "BEEF"))
        assertTrue(nameMatchesQuery(r, "Noodles"))
    }

    @Test
    fun nameMatch_chineseSubstring() {
        assertTrue(nameMatchesQuery(r, "牛肉"))
    }

    @Test
    fun nameMatch_germanIsSearchable() {
        assertTrue(nameMatchesQuery(r, "rind"))
    }

    @Test
    fun nameMatch_noMatch() {
        assertFalse(nameMatchesQuery(r, "sushi"))
    }

    @Test
    fun nameMatch_blankQueryMatchesEverything() {
        assertTrue(nameMatchesQuery(r, ""))
        assertTrue(nameMatchesQuery(r, "   "))
    }

    @Test
    fun nameMatch_nullGermanDoesNotCrash() {
        val noGerman = restaurant(name = localizable(en = "Dumplings", zh = "饺子", de = null))
        assertFalse(nameMatchesQuery(noGerman, "rind"))
        assertTrue(nameMatchesQuery(noGerman, "dump"))
    }
}
