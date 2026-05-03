package com.novawerk.berlinfoodmap.ui.pages.map

import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.TagFamily
import com.novawerk.berlinfoodmap.domain.restaurant.family

/**
 * Tags to surface on the map cards: regional only when at least one
 * regional tag exists, otherwise the format tags. Avoids the noisy
 * "川菜·火锅·麻辣烫·面食" line that 4-tagged restaurants were producing
 * while still labelling format-only places (street-food / BBQ-only).
 */
internal fun Restaurant.cardTags(): List<Tag> {
    val regional = tags.filter { it.family() == TagFamily.REGIONAL }
    return regional.ifEmpty { tags }
}
