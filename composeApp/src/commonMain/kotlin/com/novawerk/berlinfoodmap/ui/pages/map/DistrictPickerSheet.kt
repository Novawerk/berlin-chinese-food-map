package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.districts_title
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.ui.components.districtDescription
import eu.buney.maps.LatLng
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom-sheet picker for Berlin neighbourhoods. Tapping a row pans the map
 * camera to the centroid of restaurants in that district and dismisses.
 *
 * This is *not* a filter — it's a navigation shortcut. Lives in its own
 * sheet so the filter sheet can stay focused on cuisine + format selection.
 *
 * Counts respect the currently active cuisine + format filters so the user
 * sees how many places they'd land on if they jumped to that district.
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.districts_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                districts.forEach { entry ->
                    DistrictRow(
                        title = entry.name,
                        subtitle = districtDescription(entry.name),
                        count = entry.count,
                        onClick = { onDistrictSelected(entry.center) },
                    )
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
private fun DistrictRow(
    title: String,
    subtitle: String?,
    count: Int,
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
