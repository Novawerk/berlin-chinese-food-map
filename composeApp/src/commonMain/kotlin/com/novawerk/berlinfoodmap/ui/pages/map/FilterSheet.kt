package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.filter_district
import berlinfoodmap.composeapp.generated.resources.filter_reset
import berlinfoodmap.composeapp.generated.resources.filter_title
import berlinfoodmap.composeapp.generated.resources.tag_family_format
import berlinfoodmap.composeapp.generated.resources.tag_family_regional
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.TagFamily
import com.novawerk.berlinfoodmap.domain.restaurant.family
import com.novawerk.berlinfoodmap.ui.components.districtDescription
import com.novawerk.berlinfoodmap.ui.components.tagDescription
import com.novawerk.berlinfoodmap.ui.components.tagDisplayName
import eu.buney.maps.LatLng
import org.jetbrains.compose.resources.stringResource

/**
 * Tabbed filter sheet — three lists (Cuisine / Format / District), each
 * row shows a one-line description plus a result count.
 *
 * Cuisine and Format are independent single-selects (one of each can be
 * active at a time); the count next to each row reflects the number of
 * restaurants you would see if you tapped that row, given the *other*
 * family's current selection.
 *
 * District rows are not filters — tapping a district pans the map to its
 * centroid via [onDistrictSelected] and dismisses the sheet. Counts there
 * are raw per-district totals within the active cuisine+format filter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(
    allRestaurants: List<Restaurant>,
    selectedCuisine: Tag?,
    selectedFormat: Tag?,
    onCuisineSelected: (Tag?) -> Unit,
    onFormatSelected: (Tag?) -> Unit,
    onDistrictSelected: (LatLng) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(0) }

    val cuisineTags = remember { Tag.entries.filter { it.family() == TagFamily.REGIONAL } }
    val formatTags = remember { Tag.entries.filter { it.family() == TagFamily.FORMAT } }

    val cuisineCounts = remember(allRestaurants, selectedFormat) {
        cuisineTags.associateWith { tag ->
            allRestaurants.count { r ->
                tag in r.tags && (selectedFormat == null || selectedFormat in r.tags)
            }
        }
    }
    val formatCounts = remember(allRestaurants, selectedCuisine) {
        formatTags.associateWith { tag ->
            allRestaurants.count { r ->
                tag in r.tags && (selectedCuisine == null || selectedCuisine in r.tags)
            }
        }
    }

    val districts = remember(allRestaurants, selectedCuisine, selectedFormat) {
        allRestaurants
            .filter { r ->
                (selectedCuisine == null || selectedCuisine in r.tags) &&
                    (selectedFormat == null || selectedFormat in r.tags)
            }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Override Material 3's default tinted `surfaceContainerLow` so the
        // sheet picks up the brand cream surface — without this it picks up
        // a primary-tinted overlay that reads faintly purple against
        // PinwoCream.
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.filter_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onReset) {
                    Text(stringResource(Res.string.filter_reset))
                }
            }

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(Res.string.tag_family_regional)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(Res.string.tag_family_format)) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(Res.string.filter_district)) },
                )
            }

            // Plain Column + verticalScroll plays nicely with
            // ModalBottomSheet's own drag-to-expand. With ~35 districts max
            // this is cheap enough not to need LazyColumn.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                when (selectedTab) {
                    0 -> cuisineTags.forEach { tag ->
                        FilterRow(
                            title = tagDisplayName(tag),
                            subtitle = tagDescription(tag),
                            count = cuisineCounts[tag] ?: 0,
                            selected = tag == selectedCuisine,
                            onClick = {
                                onCuisineSelected(if (tag == selectedCuisine) null else tag)
                            },
                        )
                    }
                    1 -> formatTags.forEach { tag ->
                        FilterRow(
                            title = tagDisplayName(tag),
                            subtitle = tagDescription(tag),
                            count = formatCounts[tag] ?: 0,
                            selected = tag == selectedFormat,
                            onClick = {
                                onFormatSelected(if (tag == selectedFormat) null else tag)
                            },
                        )
                    }
                    else -> districts.forEach { entry ->
                        FilterRow(
                            title = entry.name,
                            subtitle = districtDescription(entry.name),
                            count = entry.count,
                            selected = false,
                            onClick = { onDistrictSelected(entry.center) },
                        )
                    }
                }
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
private fun FilterRow(
    title: String,
    subtitle: String?,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (selected) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            color = if (count == 0) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(percent = 50),
                )
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}
