package com.novawerk.berlinfoodmap.domain.restaurant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TagTest {

    @Test
    fun taxonomyIsTenRegionalPlusTwelveFormat() {
        // The 22-tag taxonomy is canonical (see CLAUDE.md / data/_tags.yaml).
        assertEquals(22, Tag.entries.size)
        assertEquals(10, Tag.entries.count { it.family() == TagFamily.REGIONAL })
        assertEquals(12, Tag.entries.count { it.family() == TagFamily.FORMAT })
    }

    @Test
    fun everyTagHasAFamily() {
        // family() does getValue() — a missing mapping would throw here.
        Tag.entries.forEach { it.family() }
    }

    @Test
    fun regionalAndFormatExamplesAreClassified() {
        assertEquals(TagFamily.REGIONAL, Tag.SICHUAN.family())
        assertEquals(TagFamily.REGIONAL, Tag.CANTONESE.family())
        assertEquals(TagFamily.FORMAT, Tag.HOTPOT.family())
        assertEquals(TagFamily.FORMAT, Tag.BBQ.family())
    }

    @Test
    fun tagFromString_roundTripsKnownNames() {
        assertEquals(Tag.SICHUAN, tagFromString("SICHUAN"))
        assertEquals(Tag.DIM_SUM, tagFromString("DIM_SUM"))
    }

    @Test
    fun tagFromString_returnsNullForUnknownOrBlank() {
        assertNull(tagFromString("CAJUN"))
        assertNull(tagFromString(""))
        assertNull(tagFromString(null))
        assertNull(tagFromString("sichuan")) // case-sensitive enum name
    }
}
