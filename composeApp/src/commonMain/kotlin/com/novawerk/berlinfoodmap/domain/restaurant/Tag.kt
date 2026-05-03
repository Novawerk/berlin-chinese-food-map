package com.novawerk.berlinfoodmap.domain.restaurant

/**
 * Restaurant tag — used to categorise restaurants by regional cuisine and/or
 * format. A restaurant carries 1~3 tags (see [Restaurant.tags]); the first
 * tag is treated as the "primary" one for default filter behaviour.
 *
 * Two families share the same enum (kept flat so the UI's chip filter is a
 * single multi-select):
 *
 *   Regional — pick at most one per restaurant.
 *   Format   — pick one or two per restaurant.
 *
 * Keep [TAG_FAMILY] in sync when adding new entries.
 */
enum class Tag {
    // Regional cuisines
    SICHUAN, CANTONESE, NORTHERN, NORTHEASTERN,
    SHANGHAINESE, HUNAN, XINJIANG, TAIWANESE, MUSLIM,

    // Formats / what they serve
    HOTPOT, BBQ, NOODLES, DUMPLINGS, DIM_SUM, MALATANG,
    VEGETARIAN, BREAKFAST, TEA_HOUSE, BAKERY, STREET_FOOD, FUSION,
}

enum class TagFamily { REGIONAL, FORMAT }

val TAG_FAMILY: Map<Tag, TagFamily> = mapOf(
    Tag.SICHUAN to TagFamily.REGIONAL,
    Tag.CANTONESE to TagFamily.REGIONAL,
    Tag.NORTHERN to TagFamily.REGIONAL,
    Tag.NORTHEASTERN to TagFamily.REGIONAL,
    Tag.SHANGHAINESE to TagFamily.REGIONAL,
    Tag.HUNAN to TagFamily.REGIONAL,
    Tag.XINJIANG to TagFamily.REGIONAL,
    Tag.TAIWANESE to TagFamily.REGIONAL,
    Tag.MUSLIM to TagFamily.REGIONAL,

    Tag.HOTPOT to TagFamily.FORMAT,
    Tag.BBQ to TagFamily.FORMAT,
    Tag.NOODLES to TagFamily.FORMAT,
    Tag.DUMPLINGS to TagFamily.FORMAT,
    Tag.DIM_SUM to TagFamily.FORMAT,
    Tag.MALATANG to TagFamily.FORMAT,
    Tag.VEGETARIAN to TagFamily.FORMAT,
    Tag.BREAKFAST to TagFamily.FORMAT,
    Tag.TEA_HOUSE to TagFamily.FORMAT,
    Tag.BAKERY to TagFamily.FORMAT,
    Tag.STREET_FOOD to TagFamily.FORMAT,
    Tag.FUSION to TagFamily.FORMAT,
)

fun Tag.family(): TagFamily = TAG_FAMILY.getValue(this)

/**
 * Parse a Firestore string into a [Tag], returning null for unknown values
 * (so old/unsupported tags don't crash the app while data is migrating).
 */
fun tagFromString(value: String?): Tag? {
    if (value.isNullOrBlank()) return null
    return runCatching { Tag.valueOf(value) }.getOrNull()
}
