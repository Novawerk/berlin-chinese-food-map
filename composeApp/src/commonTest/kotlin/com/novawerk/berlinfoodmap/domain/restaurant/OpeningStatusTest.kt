package com.novawerk.berlinfoodmap.domain.restaurant

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Minute-of-week is Sunday=0. Reference points used below:
 *   Monday 08:00 = 1*1440 + 480  = 1920
 *   Monday 09:00 = 1*1440 + 540  = 1980
 *   Monday 10:00 = 1*1440 + 600  = 2040
 *   Monday 17:00 = 1*1440 + 1020 = 2460
 *   Monday 18:00 = 1*1440 + 1080 = 2520
 *   Saturday 23:00 = 6*1440 + 1380 = 10020
 *   Sunday 01:00 = 60 ; Sunday 02:00 = 120
 */
class OpeningStatusTest {

    private val monNineToFive = listOf("1980-2460")

    @Test
    fun parse_returnsDecodedPeriods() {
        val parsed = parseOpeningPeriods(listOf("1980-2460", "0-120"))
        assertEquals(listOf(OpeningPeriod(1980, 2460), OpeningPeriod(0, 120)), parsed)
    }

    @Test
    fun parse_dropsMalformedEntriesButKeepsValidOnes() {
        assertEquals(listOf(OpeningPeriod(1980, 2460)), parseOpeningPeriods(listOf("1980-2460", "bad")))
    }

    @Test
    fun parse_emptyInput_isNull() {
        assertNull(parseOpeningPeriods(emptyList()))
    }

    @Test
    fun parse_allMalformed_isNull() {
        assertNull(parseOpeningPeriods(listOf("abc", "1-2-3")))
    }

    @Test
    fun parse_alwaysOpenToken_isEmptyListNotNull() {
        assertEquals(emptyList(), parseOpeningPeriods(listOf("OPEN_24H")))
    }

    @Test
    fun isAlwaysOpen_onlyForLoneSentinel() {
        assertTrue(isAlwaysOpen(listOf("OPEN_24H")))
        assertTrue(!isAlwaysOpen(listOf("OPEN_24H", "1980-2460")))
        assertTrue(!isAlwaysOpen(emptyList()))
        assertTrue(!isAlwaysOpen(listOf("1980-2460")))
    }

    @Test
    fun compute_emptyPeriods_isUnknown() {
        assertIs<OpeningStatus.Unknown>(computeOpeningStatus(emptyList(), nowMow = 2040))
    }

    @Test
    fun compute_unparseablePeriods_isUnknown() {
        assertIs<OpeningStatus.Unknown>(computeOpeningStatus(listOf("nonsense"), nowMow = 2040))
    }

    @Test
    fun compute_alwaysOpen() {
        assertIs<OpeningStatus.AlwaysOpen>(computeOpeningStatus(listOf("OPEN_24H"), nowMow = 0))
    }

    @Test
    fun compute_insidePeriod_isOpenWithMinutesUntilClose() {
        val status = computeOpeningStatus(monNineToFive, nowMow = 2040) // Mon 10:00
        val open = assertIs<OpeningStatus.Open>(status)
        assertEquals(2460, open.closeMow)
        assertEquals(420, open.minutesUntilClose) // 17:00 - 10:00 = 7h
    }

    @Test
    fun compute_beforeOpening_isClosedWithMinutesUntilOpen() {
        val status = computeOpeningStatus(monNineToFive, nowMow = 1920) // Mon 08:00
        val closed = assertIs<OpeningStatus.Closed>(status)
        assertEquals(1980, closed.nextOpenMow)
        assertEquals(60, closed.minutesUntilOpen)
    }

    @Test
    fun compute_afterClosing_wrapsToNextWeek() {
        val status = computeOpeningStatus(monNineToFive, nowMow = 2520) // Mon 18:00
        val closed = assertIs<OpeningStatus.Closed>(status)
        assertEquals(1980, closed.nextOpenMow)
        // From Mon 18:00 the next opening is next Mon 09:00 = 6 days 15h later.
        assertEquals(9540, closed.minutesUntilOpen)
    }

    @Test
    fun compute_periodSpanningWeekBoundary_openOnSaturdayNight() {
        // Sat 23:00 -> Sun 02:00, encoded as open 10020, close 120 (close < open).
        val periods = listOf("10020-120")
        val status = computeOpeningStatus(periods, nowMow = 10050) // Sat 23:30
        val open = assertIs<OpeningStatus.Open>(status)
        assertEquals(120, open.closeMow)
        assertEquals(150, open.minutesUntilClose) // until Sun 02:00
    }

    @Test
    fun compute_periodSpanningWeekBoundary_openAfterMidnight() {
        val periods = listOf("10020-120")
        val status = computeOpeningStatus(periods, nowMow = 60) // Sun 01:00
        val open = assertIs<OpeningStatus.Open>(status)
        assertEquals(120, open.closeMow)
        assertEquals(60, open.minutesUntilClose)
    }

    @Test
    fun compute_multiplePeriods_picksSoonestUpcoming() {
        // Mon 09:00-12:00 and Mon 14:00-17:00; now is the 13:00 lunch gap.
        val periods = listOf("1980-2160", "2280-2460")
        val status = computeOpeningStatus(periods, nowMow = 2220) // Mon 13:00
        val closed = assertIs<OpeningStatus.Closed>(status)
        assertEquals(2280, closed.nextOpenMow) // afternoon session
        assertEquals(60, closed.minutesUntilOpen)
    }

    @Test
    fun mowToTimeOfDay_dropsDayComponent() {
        assertEquals(LocalTime(10, 0), mowToTimeOfDay(2040)) // Mon 10:00
        assertEquals(LocalTime(2, 0), mowToTimeOfDay(120))   // Sun 02:00
    }

    @Test
    fun mowToDayIndex_isSundayBased() {
        assertEquals(0, mowToDayIndex(120))    // Sunday
        assertEquals(1, mowToDayIndex(2040))   // Monday
        assertEquals(6, mowToDayIndex(10020))  // Saturday
    }

    @Test
    fun currentBerlinMinuteOfWeek_isWithinTheWeek() {
        val mow = currentBerlinMinuteOfWeek()
        assertTrue(mow in 0 until 7 * 24 * 60, "mow out of range: $mow")
    }
}
