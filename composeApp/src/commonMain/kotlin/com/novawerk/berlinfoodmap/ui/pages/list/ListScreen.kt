package com.novawerk.berlinfoodmap.ui.pages.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.ui.components.EmptyState
import com.novawerk.berlinfoodmap.ui.components.RestaurantCard
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.search_hint
import berlinfoodmap.composeapp.generated.resources.sort_most_visited
import berlinfoodmap.composeapp.generated.resources.sort_name
import berlinfoodmap.composeapp.generated.resources.no_results

private enum class SortMode { MOST_VISITED, NAME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    repository: RestaurantRepository,
    onNavigateDetail: (String) -> Unit,
    onNavigateSearch: () -> Unit,
) {
    var restaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var sortMode by remember { mutableStateOf(SortMode.MOST_VISITED) }

    LaunchedEffect(Unit) {
        restaurants = repository.getAll()
        loading = false
    }

    val sortedRestaurants = remember(restaurants, sortMode) {
        when (sortMode) {
            SortMode.MOST_VISITED -> restaurants.sortedByDescending { it.visitCount }
            SortMode.NAME -> restaurants.sortedBy { it.name.en.lowercase() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Clickable search bar
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            placeholder = { Text(stringResource(Res.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onNavigateSearch() },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        // Sort chips
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = sortMode == SortMode.MOST_VISITED,
                onClick = { sortMode = SortMode.MOST_VISITED },
                label = { Text(stringResource(Res.string.sort_most_visited)) },
            )
            FilterChip(
                selected = sortMode == SortMode.NAME,
                onClick = { sortMode = SortMode.NAME },
                label = { Text(stringResource(Res.string.sort_name)) },
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (sortedRestaurants.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.SearchOff,
                title = stringResource(Res.string.no_results),
                subtitle = "",
            )
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sortedRestaurants, key = { it.id }) { restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    onClick = { onNavigateDetail(restaurant.id) },
                )
            }
        }
    }
}
