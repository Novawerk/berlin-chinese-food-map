package com.novawerk.berlinfoodmap.domain.restaurant

import kotlin.test.Test
import kotlin.test.assertEquals

class PinwoLinksTest {
    private val utm = "utm_source=berlinfoodmap&utm_medium=app&utm_campaign=coupon"

    @Test
    fun blankUrlFallsBackToHomepage() {
        assertEquals("https://pinwo.de?$utm", pinwoAppLink(null))
        assertEquals("https://pinwo.de?$utm", pinwoAppLink("  "))
    }

    @Test
    fun couponUrlWithoutQueryGetsUtm() {
        assertEquals(
            "https://pinwo.de/coupons/ni-hao?$utm",
            pinwoAppLink(" https://pinwo.de/coupons/ni-hao "),
        )
    }

    @Test
    fun existingQueryIsKept() {
        assertEquals(
            "https://pinwo.de/coupon?id=42&$utm",
            pinwoAppLink("https://pinwo.de/coupon?id=42"),
        )
    }

    @Test
    fun fragmentStaysAtTheEnd() {
        assertEquals(
            "https://pinwo.de/coupon?id=42&$utm#top",
            pinwoAppLink("https://pinwo.de/coupon?id=42#top"),
        )
    }

    @Test
    fun existingUtmKeyIsNotOverridden() {
        assertEquals(
            "https://pinwo.de/c?utm_source=x&utm_medium=app&utm_campaign=coupon",
            pinwoAppLink("https://pinwo.de/c?utm_source=x"),
        )
    }
}
