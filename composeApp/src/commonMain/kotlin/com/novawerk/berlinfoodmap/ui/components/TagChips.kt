package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.TagFamily
import com.novawerk.berlinfoodmap.domain.restaurant.family
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.all
import berlinfoodmap.composeapp.generated.resources.tag_bakery
import berlinfoodmap.composeapp.generated.resources.tag_bbq
import berlinfoodmap.composeapp.generated.resources.tag_breakfast
import berlinfoodmap.composeapp.generated.resources.tag_cantonese
import berlinfoodmap.composeapp.generated.resources.tag_dim_sum
import berlinfoodmap.composeapp.generated.resources.tag_dumplings
import berlinfoodmap.composeapp.generated.resources.tag_fusion
import berlinfoodmap.composeapp.generated.resources.tag_hotpot
import berlinfoodmap.composeapp.generated.resources.tag_hunan
import berlinfoodmap.composeapp.generated.resources.tag_malatang
import berlinfoodmap.composeapp.generated.resources.tag_muslim
import berlinfoodmap.composeapp.generated.resources.tag_noodles
import berlinfoodmap.composeapp.generated.resources.tag_northeastern
import berlinfoodmap.composeapp.generated.resources.tag_northern
import berlinfoodmap.composeapp.generated.resources.tag_shanghainese
import berlinfoodmap.composeapp.generated.resources.tag_sichuan
import berlinfoodmap.composeapp.generated.resources.tag_street_food
import berlinfoodmap.composeapp.generated.resources.tag_taiwanese
import berlinfoodmap.composeapp.generated.resources.tag_tea_house
import berlinfoodmap.composeapp.generated.resources.tag_vegetarian
import berlinfoodmap.composeapp.generated.resources.tag_xinjiang

/**
 * Inline horizontally-scrollable tag filter. Multi-select; tapping the
 * "All" chip clears the selection.
 *
 * Used on screens (e.g. Search) where vertical space is constrained. For a
 * grouped, FlowRow-style picker prefer [TagChipsGroup].
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
        )
        // Stable order: regional first, then format, in declaration order.
        Tag.entries
            .sortedBy { if (it.family() == TagFamily.REGIONAL) 0 else 1 }
            .forEach { tag ->
                FilterChip(
                    selected = tag in selected,
                    onClick = { onToggle(tag) },
                    label = { Text(tagDisplayName(tag)) },
                )
            }
    }
}

/**
 * FlowRow variant that splits tags into Regional / Format sections. Use this
 * inside the bottom-sheet filter where we have room to lay them out in two
 * captioned blocks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChipsGroup(
    selected: Set<Tag>,
    onToggle: (Tag) -> Unit,
    onClear: () -> Unit,
    regionalLabel: String,
    formatLabel: String,
    allLabel: String = stringResource(Res.string.all),
    modifier: Modifier = Modifier,
) {
    val regional = Tag.entries.filter { it.family() == TagFamily.REGIONAL }
    val format = Tag.entries.filter { it.family() == TagFamily.FORMAT }

    androidx.compose.foundation.layout.Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = selected.isEmpty(),
                onClick = onClear,
                label = { Text(allLabel) },
            )
        }
        SectionHeader(regionalLabel)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            regional.forEach { tag ->
                FilterChip(
                    selected = tag in selected,
                    onClick = { onToggle(tag) },
                    label = { Text(tagDisplayName(tag)) },
                )
            }
        }
        SectionHeader(formatLabel)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            format.forEach { tag ->
                FilterChip(
                    selected = tag in selected,
                    onClick = { onToggle(tag) },
                    label = { Text(tagDisplayName(tag)) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
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
