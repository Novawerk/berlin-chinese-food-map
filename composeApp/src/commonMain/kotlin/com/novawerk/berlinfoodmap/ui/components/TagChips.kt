package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.TagFamily
import com.novawerk.berlinfoodmap.domain.restaurant.family
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.all
import berlinfoodmap.composeapp.generated.resources.tag_bakery
import berlinfoodmap.composeapp.generated.resources.tag_bakery_desc
import berlinfoodmap.composeapp.generated.resources.tag_bbq
import berlinfoodmap.composeapp.generated.resources.tag_bbq_desc
import berlinfoodmap.composeapp.generated.resources.tag_breakfast
import berlinfoodmap.composeapp.generated.resources.tag_breakfast_desc
import berlinfoodmap.composeapp.generated.resources.tag_cantonese
import berlinfoodmap.composeapp.generated.resources.tag_cantonese_desc
import berlinfoodmap.composeapp.generated.resources.tag_dim_sum
import berlinfoodmap.composeapp.generated.resources.tag_dim_sum_desc
import berlinfoodmap.composeapp.generated.resources.tag_dumplings
import berlinfoodmap.composeapp.generated.resources.tag_dumplings_desc
import berlinfoodmap.composeapp.generated.resources.tag_fusion
import berlinfoodmap.composeapp.generated.resources.tag_fusion_desc
import berlinfoodmap.composeapp.generated.resources.tag_hotpot
import berlinfoodmap.composeapp.generated.resources.tag_hotpot_desc
import berlinfoodmap.composeapp.generated.resources.tag_hunan
import berlinfoodmap.composeapp.generated.resources.tag_hunan_desc
import berlinfoodmap.composeapp.generated.resources.tag_malatang
import berlinfoodmap.composeapp.generated.resources.tag_malatang_desc
import berlinfoodmap.composeapp.generated.resources.tag_mongolian
import berlinfoodmap.composeapp.generated.resources.tag_mongolian_desc
import berlinfoodmap.composeapp.generated.resources.tag_muslim
import berlinfoodmap.composeapp.generated.resources.tag_muslim_desc
import berlinfoodmap.composeapp.generated.resources.tag_noodles
import berlinfoodmap.composeapp.generated.resources.tag_noodles_desc
import berlinfoodmap.composeapp.generated.resources.tag_northeastern
import berlinfoodmap.composeapp.generated.resources.tag_northeastern_desc
import berlinfoodmap.composeapp.generated.resources.tag_northern
import berlinfoodmap.composeapp.generated.resources.tag_northern_desc
import berlinfoodmap.composeapp.generated.resources.tag_shanghainese
import berlinfoodmap.composeapp.generated.resources.tag_shanghainese_desc
import berlinfoodmap.composeapp.generated.resources.tag_sichuan
import berlinfoodmap.composeapp.generated.resources.tag_sichuan_desc
import berlinfoodmap.composeapp.generated.resources.tag_street_food
import berlinfoodmap.composeapp.generated.resources.tag_street_food_desc
import berlinfoodmap.composeapp.generated.resources.tag_taiwanese
import berlinfoodmap.composeapp.generated.resources.tag_taiwanese_desc
import berlinfoodmap.composeapp.generated.resources.tag_tea_house
import berlinfoodmap.composeapp.generated.resources.tag_tea_house_desc
import berlinfoodmap.composeapp.generated.resources.tag_vegetarian
import berlinfoodmap.composeapp.generated.resources.tag_vegetarian_desc
import berlinfoodmap.composeapp.generated.resources.tag_xinjiang
import berlinfoodmap.composeapp.generated.resources.tag_xinjiang_desc

/** testTag for the "All" (clear selection) chip. */
const val TAG_CHIP_ALL = "tagChip_all"

/** Stable testTag for a tag's filter chip, e.g. `tagChip_SICHUAN`. */
fun tagChipTestTag(tag: Tag): String = "tagChip_${tag.name}"

/**
 * Inline horizontally-scrollable tag filter. Multi-select; tapping the
 * "All" chip clears the selection.
 *
 * Used on screens (e.g. Search) where vertical space is constrained.
 */
@Composable
fun TagChips(
    selected: Set<Tag>,
    onToggle: (Tag) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected.isEmpty(),
            onClick = onClear,
            label = { Text(stringResource(Res.string.all)) },
            modifier = Modifier.testTag(TAG_CHIP_ALL),
        )
        // Stable order: regional first, then format, in declaration order.
        Tag.entries
            .sortedBy { if (it.family() == TagFamily.REGIONAL) 0 else 1 }
            .forEach { tag ->
                FilterChip(
                    selected = tag in selected,
                    onClick = { onToggle(tag) },
                    label = { Text(tagDisplayName(tag)) },
                    modifier = Modifier.testTag(tagChipTestTag(tag)),
                )
            }
    }
}

@Composable
fun tagDisplayName(tag: Tag): String = when (tag) {
    Tag.SICHUAN -> stringResource(Res.string.tag_sichuan)
    Tag.CANTONESE -> stringResource(Res.string.tag_cantonese)
    Tag.NORTHERN -> stringResource(Res.string.tag_northern)
    Tag.NORTHEASTERN -> stringResource(Res.string.tag_northeastern)
    Tag.SHANGHAINESE -> stringResource(Res.string.tag_shanghainese)
    Tag.HUNAN -> stringResource(Res.string.tag_hunan)
    Tag.XINJIANG -> stringResource(Res.string.tag_xinjiang)
    Tag.TAIWANESE -> stringResource(Res.string.tag_taiwanese)
    Tag.MUSLIM -> stringResource(Res.string.tag_muslim)
    Tag.MONGOLIAN -> stringResource(Res.string.tag_mongolian)
    Tag.HOTPOT -> stringResource(Res.string.tag_hotpot)
    Tag.BBQ -> stringResource(Res.string.tag_bbq)
    Tag.NOODLES -> stringResource(Res.string.tag_noodles)
    Tag.DUMPLINGS -> stringResource(Res.string.tag_dumplings)
    Tag.DIM_SUM -> stringResource(Res.string.tag_dim_sum)
    Tag.MALATANG -> stringResource(Res.string.tag_malatang)
    Tag.VEGETARIAN -> stringResource(Res.string.tag_vegetarian)
    Tag.BREAKFAST -> stringResource(Res.string.tag_breakfast)
    Tag.TEA_HOUSE -> stringResource(Res.string.tag_tea_house)
    Tag.BAKERY -> stringResource(Res.string.tag_bakery)
    Tag.STREET_FOOD -> stringResource(Res.string.tag_street_food)
    Tag.FUSION -> stringResource(Res.string.tag_fusion)
}

fun tagPhoto(tag: Tag): DrawableResource = when (tag) {
    Tag.SICHUAN -> Res.drawable.tag_sichuan
    Tag.CANTONESE -> Res.drawable.tag_cantonese
    Tag.NORTHERN -> Res.drawable.tag_northern
    Tag.NORTHEASTERN -> Res.drawable.tag_northeastern
    Tag.SHANGHAINESE -> Res.drawable.tag_shanghainese
    Tag.HUNAN -> Res.drawable.tag_hunan
    Tag.XINJIANG -> Res.drawable.tag_xinjiang
    Tag.TAIWANESE -> Res.drawable.tag_taiwanese
    Tag.MUSLIM -> Res.drawable.tag_muslim
    Tag.MONGOLIAN -> Res.drawable.tag_mongolian
    Tag.HOTPOT -> Res.drawable.tag_hotpot
    Tag.BBQ -> Res.drawable.tag_bbq
    Tag.NOODLES -> Res.drawable.tag_noodles
    Tag.DUMPLINGS -> Res.drawable.tag_dumplings
    Tag.DIM_SUM -> Res.drawable.tag_dim_sum
    Tag.MALATANG -> Res.drawable.tag_malatang
    Tag.VEGETARIAN -> Res.drawable.tag_vegetarian
    Tag.BREAKFAST -> Res.drawable.tag_breakfast
    Tag.TEA_HOUSE -> Res.drawable.tag_tea_house
    Tag.BAKERY -> Res.drawable.tag_bakery
    Tag.STREET_FOOD -> Res.drawable.tag_street_food
    Tag.FUSION -> Res.drawable.tag_fusion
}

@Composable
fun tagDescription(tag: Tag): String = when (tag) {
    Tag.SICHUAN -> stringResource(Res.string.tag_sichuan_desc)
    Tag.CANTONESE -> stringResource(Res.string.tag_cantonese_desc)
    Tag.NORTHERN -> stringResource(Res.string.tag_northern_desc)
    Tag.NORTHEASTERN -> stringResource(Res.string.tag_northeastern_desc)
    Tag.SHANGHAINESE -> stringResource(Res.string.tag_shanghainese_desc)
    Tag.HUNAN -> stringResource(Res.string.tag_hunan_desc)
    Tag.XINJIANG -> stringResource(Res.string.tag_xinjiang_desc)
    Tag.TAIWANESE -> stringResource(Res.string.tag_taiwanese_desc)
    Tag.MUSLIM -> stringResource(Res.string.tag_muslim_desc)
    Tag.MONGOLIAN -> stringResource(Res.string.tag_mongolian_desc)
    Tag.HOTPOT -> stringResource(Res.string.tag_hotpot_desc)
    Tag.BBQ -> stringResource(Res.string.tag_bbq_desc)
    Tag.NOODLES -> stringResource(Res.string.tag_noodles_desc)
    Tag.DUMPLINGS -> stringResource(Res.string.tag_dumplings_desc)
    Tag.DIM_SUM -> stringResource(Res.string.tag_dim_sum_desc)
    Tag.MALATANG -> stringResource(Res.string.tag_malatang_desc)
    Tag.VEGETARIAN -> stringResource(Res.string.tag_vegetarian_desc)
    Tag.BREAKFAST -> stringResource(Res.string.tag_breakfast_desc)
    Tag.TEA_HOUSE -> stringResource(Res.string.tag_tea_house_desc)
    Tag.BAKERY -> stringResource(Res.string.tag_bakery_desc)
    Tag.STREET_FOOD -> stringResource(Res.string.tag_street_food_desc)
    Tag.FUSION -> stringResource(Res.string.tag_fusion_desc)
}
