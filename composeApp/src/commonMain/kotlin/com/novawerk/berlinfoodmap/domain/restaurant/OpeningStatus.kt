package com.novawerk.berlinfoodmap.domain.restaurant

import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val MINUTES_PER_DAY = 1440
private const val MINUTES_PER_WEEK = MINUTES_PER_DAY * 7

private val BERLIN_TZ = TimeZone.of("Europe/Berlin")

/**
 * One opening period encoded as minute-of-week (Sunday = 0). [closeMow] may
 * be smaller than [openMow] when the period spans midnight into the next day —
 * callers must normalise by adding [MINUTES_PER_WEEK].
 */
data class OpeningPeriod(val openMow: Int, val closeMow: Int)

sealed interface OpeningStatus {
    data object AlwaysOpen : OpeningStatus
    /** Currently inside an opening period. [closeMow] is normalised to [0, MINUTES_PER_WEEK). */
    data class Open(val closeMow: Int, val minutesUntilClose: Int) : OpeningStatus
    /** Outside every opening period. [nextOpenMow] is normalised to [0, MINUTES_PER_WEEK). */
    data class Closed(val nextOpenMow: Int, val minutesUntilOpen: Int) : OpeningStatus
    /** No structured periods available — UI should fall back to weekdayText only. */
    data object Unknown : OpeningStatus
}

/** Decoded periods, or null when the raw data isn't usable. */
fun parseOpeningPeriods(raw: List<String>): List<OpeningPeriod>? {
    if (raw.isEmpty()) return null
    if (raw.size == 1 && raw[0] == ALWAYS_OPEN_TOKEN) return emptyList()
    val out = raw.mapNotNull { line ->
        val parts = line.split('-')
        if (parts.size != 2) return@mapNotNull null
        val o = parts[0].toIntOrNull() ?: return@mapNotNull null
        val c = parts[1].toIntOrNull() ?: return@mapNotNull null
        OpeningPeriod(o, c)
    }
    return out.takeIf { it.isNotEmpty() }
}

/** True when [parseOpeningPeriods] would interpret [raw] as 24/7. */
fun isAlwaysOpen(raw: List<String>): Boolean =
    raw.size == 1 && raw[0] == ALWAYS_OPEN_TOKEN

fun computeOpeningStatus(
    raw: List<String>,
    nowMow: Int = currentBerlinMinuteOfWeek(),
): OpeningStatus {
    if (raw.isEmpty()) return OpeningStatus.Unknown
    if (isAlwaysOpen(raw)) return OpeningStatus.AlwaysOpen
    val periods = parseOpeningPeriods(raw) ?: return OpeningStatus.Unknown

    // 1) Are we currently inside any period?
    for (p in periods) {
        val end = if (p.closeMow >= p.openMow) p.closeMow else p.closeMow + MINUTES_PER_WEEK
        // Match either this week's window or last week's wrapped tail.
        val n = if (nowMow >= p.openMow) nowMow else nowMow + MINUTES_PER_WEEK
        if (n in p.openMow until end) {
            val minutesLeft = end - n
            val closeMow = ((p.closeMow % MINUTES_PER_WEEK) + MINUTES_PER_WEEK) % MINUTES_PER_WEEK
            return OpeningStatus.Open(closeMow, minutesLeft)
        }
    }

    // 2) Otherwise, find the soonest upcoming opening.
    var bestDelta = Int.MAX_VALUE
    var bestOpenMow = 0
    for (p in periods) {
        val delta = ((p.openMow - nowMow) % MINUTES_PER_WEEK + MINUTES_PER_WEEK) % MINUTES_PER_WEEK
        // delta == 0 means "right now" — treat as next-week opening so a true 0
        // doesn't get reported as "opens in 0 min" while the period check above
        // is also miss. In practice the period check would have matched, so
        // delta is always >= 1 here.
        val effective = if (delta == 0) MINUTES_PER_WEEK else delta
        if (effective < bestDelta) {
            bestDelta = effective
            bestOpenMow = p.openMow
        }
    }
    if (bestDelta == Int.MAX_VALUE) return OpeningStatus.Unknown
    val nextOpenMow = ((bestOpenMow % MINUTES_PER_WEEK) + MINUTES_PER_WEEK) % MINUTES_PER_WEEK
    return OpeningStatus.Closed(nextOpenMow, bestDelta)
}

/** Current minute-of-week in Berlin, with Sunday = 0 (matching Google's `day`). */
@OptIn(ExperimentalTime::class)
fun currentBerlinMinuteOfWeek(): Int {
    val ldt = Clock.System.now().toLocalDateTime(BERLIN_TZ)
    // DayOfWeek.ordinal is Monday=0..Sunday=6; convert to Sunday=0..Saturday=6.
    val sundayBasedDay = (ldt.dayOfWeek.ordinal + 1) % 7
    return sundayBasedDay * MINUTES_PER_DAY + ldt.hour * 60 + ldt.minute
}

/** Time of day for a minute-of-week value (drops the day component). */
fun mowToTimeOfDay(mow: Int): LocalTime {
    val tod = ((mow % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
    return LocalTime(tod / 60, tod % 60)
}

/** Sunday-based day index (0..6) for a minute-of-week value. */
fun mowToDayIndex(mow: Int): Int {
    val n = ((mow % MINUTES_PER_WEEK) + MINUTES_PER_WEEK) % MINUTES_PER_WEEK
    return n / MINUTES_PER_DAY
}

private const val ALWAYS_OPEN_TOKEN = "OPEN_24H"
