package com.novawerk.berlinfoodmap.domain.common

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizableTest {

    private val both = Localizable(en = "Beef Noodles", zh = "牛肉面", de = "Rindfleischnudeln")

    @Test
    fun zhLocale_prefersChinese() {
        assertEquals("牛肉面", both.preferred("zh"))
        assertEquals("牛肉面", both.preferred("zh_CN"))
        assertEquals("牛肉面", both.preferred("ZH-Hant"))
    }

    @Test
    fun enLocale_prefersEnglish() {
        assertEquals("Beef Noodles", both.preferred("en"))
        assertEquals("Beef Noodles", both.preferred("en-US"))
    }

    @Test
    fun unknownLocale_fallsBackToEnglish() {
        assertEquals("Beef Noodles", both.preferred("fr"))
    }

    @Test
    fun zhLocale_blankZh_fallsBackToEnglish() {
        val enOnly = Localizable(en = "Dumplings", zh = "")
        assertEquals("Dumplings", enOnly.preferred("zh"))
    }

    @Test
    fun enLocale_blankEn_fallsBackToChinese() {
        val zhOnly = Localizable(en = "", zh = "饺子")
        assertEquals("饺子", zhOnly.preferred("en"))
    }

    @Test
    fun germanIsNotSelectedAsUiLanguage() {
        // `de` is restaurant-data-only — never returned by preferred().
        val withDe = Localizable(en = "Hotpot", zh = "火锅", de = "Feuertopf")
        assertEquals("Hotpot", withDe.preferred("de"))
    }
}
