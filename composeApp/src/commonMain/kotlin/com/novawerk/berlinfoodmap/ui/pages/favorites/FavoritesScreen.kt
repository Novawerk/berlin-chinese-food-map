package com.novawerk.berlinfoodmap.ui.pages.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.ui.components.EmptyState
import com.novawerk.berlinfoodmap.ui.components.RestaurantCard
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.nav_favorites
import berlinfoodmap.composeapp.generated.resources.no_favorites
import berlinfoodmap.composeapp.generated.resources.no_favorites_hint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    repository: RestaurantRepository,
    favoritesRepository: FavoritesRepository,
    onNavigateDetail: (String) -> Unit,
) {
    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())
    var allRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        allRestaurants = repository.getAll()
        loading = false
    }

    val favoriteRestaurants = remember(allRestaurants, favoriteIds) {
        allRestaurants.filter { it.id in favoriteIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.nav_favorites)) },
            )
        },
    ) { padding ->
        if (loading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (favoriteRestaurants.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = stringResource(Res.string.no_favorites),
                subtitle = stringResource(Res.string.no_favorites_hint),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(favoriteRestaurants, key = { it.id }) { restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    onClick = { onNavigateDetail(restaurant.id) },
                )
            }
        }
    }
}
