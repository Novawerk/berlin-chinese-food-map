package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.districts_count_label
import berlinfoodmap.composeapp.generated.resources.districts_subtitle
import berlinfoodmap.composeapp.generated.resources.districts_title
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.ui.components.districtDescription
import eu.buney.maps.LatLng
import org.jetbrains.compose.resources.stringResource

private const val POPULAR_TOP_N = 3

/**
 * Ortsteile that sat in the Soviet sector during the 1961–1989 division.
 * Used purely for visual variety in the picker — gives the non-popular
 * cards a meaningful split (East = warm gold / Ampelmännchen-yellow,
 * West = sage / Tiergarten-green) instead of one homogeneous neutral.
 *
 * Berlin's modern boroughs were reshuffled in 2001, but the districts
 * stored in `Restaurant.address.district` are the original Ortsteile,
 * so this list maps cleanly. Mitte (Ortsteil, not borough) was the
 * Soviet-sector core — Museumsinsel, Brandenburger Tor — and counts as East.
 */
private val EAST_BERLIN_DISTRICTS: Set<String> = setOf(
    "Mitte",
    "Friedrichshain",
    "Prenzlauer Berg",
    "Pankow",
    "Lichtenberg",
    "Marzahn",
    "Hellersdorf",
    "Französisch Buchholz",
    "Johannisthal",
)

private enum class CardVariant { Popular, East, West }

/**
 * Bottom-sheet picker for Berlin neighbourhoods, laid out as a 2-column
 * grid. The top [POPULAR_TOP_N] districts (by restaurant count) get the
 * brand `primaryContainer` (PinwoPink) treatment with a star marker —
 * gives the sheet visual rhythm without requiring per-district artwork.
 *
 * Tapping a tile pans the camera to the centroid and dismisses the sheet.
 * This is *not* a filter — district browsing leaves the active filter
 * state alone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DistrictPickerSheet(
    restaurants: List<Restaurant>,
    onDistrictSelected: (LatLng) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val districts = remember(restaurants) {
        restaurants
            .groupBy { it.address.district }
            .map { (name, members) ->
                val avgLat = members.sumOf { it.latitude } / members.size
                val avgLng = members.sumOf { it.longitude } / members.size
                DistrictEntry(
                    name = name,
                    count = members.size,
                    center = LatLng(avgLat, avgLng),
                )
            }
            .sortedWith(compareByDescending<DistrictEntry> { it.count }.thenBy { it.name })
    }

    // The first POPULAR_TOP_N entries (post-sort) are the popular ones —
    // identifying them by name keeps the grid stable if the underlying
    // list reorders on a future fetch.
    val popularNames = remember(districts) {
        districts.take(POPULAR_TOP_N).mapTo(HashSet()) { it.name }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // Single-source-of-scroll: the grid IS the sheet content (header
        // emitted as a full-width item). An outer Column with weight on
        // the grid was previously fighting the sheet's nested scroll
        // connection at the bottom boundary, producing a vertical bounce
        // when the user reached the end of the list. Letting the grid
        // own the scroll directly hands the sheet a single nested-scroll
        // child it knows how to coordinate with.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DistrictsHeader()
            }
            items(districts, key = { it.name }) { entry ->
                DistrictCard(
                    entry = entry,
                    popular = entry.name in popularNames,
                    onClick = { onDistrictSelected(entry.center) },
                )
            }
        }
    }
}

private data class DistrictEntry(
    val name: String,
    val count: Int,
    val center: LatLng,
)

@Composable
private fun DistrictsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Pull the header back out of the grid's `contentPadding`
            // (16dp horizontal) so its left edge lines up at 20dp from
            // the screen edge — matches the FilterSheet header.
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(percent = 50),
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(Res.string.districts_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.districts_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DistrictCard(
    entry: DistrictEntry,
    popular: Boolean,
    onClick: () -> Unit,
) {
    // Three tiers:
    //  - Popular: full `primary` (PinwoRed) with white text — the theme
    //    moment. Always wins regardless of side.
    //  - East: `tertiaryContainer` (PinwoGold) — warm yellow nodding to
    //    the iconic GDR Ampelmännchen.
    //  - West: `secondaryContainer` (PinwoSage) — sage green nodding to
    //    West-Berlin parkland (Tiergarten, Grunewald).
    // Brand-red accent on the count chip and star keeps the visual rhythm
    // consistent across all three tiers.
    val variant = when {
        popular -> CardVariant.Popular
        entry.name in EAST_BERLIN_DISTRICTS -> CardVariant.East
        else -> CardVariant.West
    }
    val container = when (variant) {
        CardVariant.Popular -> MaterialTheme.colorScheme.primary
        CardVariant.East -> MaterialTheme.colorScheme.tertiaryContainer
        CardVariant.West -> MaterialTheme.colorScheme.secondaryContainer
    }
    val onContainer = when (variant) {
        CardVariant.Popular -> MaterialTheme.colorScheme.onPrimary
        CardVariant.East -> MaterialTheme.colorScheme.onTertiaryContainer
        CardVariant.West -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val accent = if (variant == CardVariant.Popular) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        color = container,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                // Top row: optional star for popular districts, count chip
                // pinned right.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (popular) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    CountChip(
                        count = entry.count,
                        accent = accent,
                        onContainer = onContainer,
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer,
                    maxLines = 1,
                )
                val description = districtDescription(entry.name)
                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer.copy(alpha = 0.75f),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun CountChip(count: Int, accent: Color, onContainer: Color) {
    Surface(
        color = onContainer.copy(alpha = 0.10f),
        shape = RoundedCornerShape(percent = 50),
    ) {
        Text(
            text = stringResource(Res.string.districts_count_label, count),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
