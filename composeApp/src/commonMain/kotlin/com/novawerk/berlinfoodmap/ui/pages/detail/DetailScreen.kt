package com.novawerk.berlinfoodmap.ui.pages.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.auth.AuthService
import com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.ui.components.cuisineDisplayName
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.back
import berlinfoodmap.composeapp.generated.resources.visited_button
import berlinfoodmap.composeapp.generated.resources.already_visited
import berlinfoodmap.composeapp.generated.resources.call_phone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    restaurantId: String,
    repository: RestaurantRepository,
    authService: AuthService,
    favoritesRepository: FavoritesRepository,
    onBack: () -> Unit,
) {
    var restaurant by remember { mutableStateOf<Restaurant?>(null) }
    var loading by remember { mutableStateOf(true) }
    var hasVisited by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Collect favorite state
    LaunchedEffect(restaurantId) {
        favoritesRepository.favoriteIds.collect { ids ->
            isFavorite = restaurantId in ids
        }
    }

    LaunchedEffect(restaurantId) {
        restaurant = repository.getById(restaurantId)
        loading = false

        // Track view
        val uid = authService.getCurrentUid()
        if (uid != null) {
            repository.incrementViewCount(restaurantId, uid)
            hasVisited = repository.hasVisited(restaurantId, uid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    restaurant?.let {
                        Text(it.name.zh)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                favoritesRepository.toggleFavorite(restaurantId)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val r = restaurant
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.back),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Gallery pager
            if (r.galleries.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { r.galleries.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                ) { page ->
                    // Placeholder for gallery images (AsyncImage would go here)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${page + 1}/${r.galleries.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Restaurant name
                Text(r.name.zh, style = MaterialTheme.typography.headlineMedium)
                Text(
                    r.name.en,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Cuisine badge + price range
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(cuisineDisplayName(r.cuisineType)) },
                    )
                    r.priceRange?.let { price ->
                        Text(
                            text = price,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider()

                // Address section
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(r.address.addressLine1, style = MaterialTheme.typography.bodyMedium)
                        r.address.addressLine2?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "${r.address.postalCode} ${r.address.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            r.address.district,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Phone
                r.phone?.let { phone ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = stringResource(Res.string.call_phone),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(phone, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Description
                r.description?.let { desc ->
                    HorizontalDivider()
                    Text(desc.zh, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        desc.en,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider()

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("${r.viewCount}", style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("${r.visitCount}", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Visit button
                Button(
                    onClick = {
                        val uid = authService.getCurrentUid() ?: return@Button
                        scope.launch {
                            try {
                                repository.markVisited(restaurantId, uid)
                                hasVisited = true
                                restaurant = repository.getById(restaurantId)
                            } catch (_: Exception) {
                            }
                        }
                    },
                    enabled = !hasVisited,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        if (hasVisited) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (hasVisited) stringResource(Res.string.already_visited)
                        else stringResource(Res.string.visited_button)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
