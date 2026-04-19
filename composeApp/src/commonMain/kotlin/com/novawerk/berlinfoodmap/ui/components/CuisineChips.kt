package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.restaurant.CuisineType
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.all

@Composable
fun CuisineChips(
    selected: CuisineType?,
    onSelected: (CuisineType?) -> Unit,
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
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text(stringResource(Res.string.all)) },
        )
        CuisineType.entries.forEach { cuisine ->
            FilterChip(
                selected = selected == cuisine,
                onClick = { onSelected(cuisine) },
                label = { Text(cuisineDisplayName(cuisine)) },
            )
        }
    }
}
