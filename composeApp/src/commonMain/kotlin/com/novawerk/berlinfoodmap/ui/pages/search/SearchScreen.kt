package com.novawerk.berlinfoodmap.ui.pages.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.tagFromString
import com.novawerk.berlinfoodmap.ui.components.EmptyState
import com.novawerk.berlinfoodmap.ui.components.RestaurantCard
import com.novawerk.berlinfoodmap.ui.components.TagChips
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.back
import berlinfoodmap.composeapp.generated.resources.search_hint
import berlinfoodmap.composeapp.generated.resources.filter_district
import berlinfoodmap.composeapp.generated.resources.all
import berlinfoodmap.composeapp.generated.resources.no_results

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: RestaurantRepository,
    initialCuisine: String?,
    initialDistrict: String?,
    onNavigateDetail: (String) -> Unit,
    onBack: () -> Unit,
) {
    var allRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selectedTags by remember {
        mutableStateOf(
            buildSet {
                tagFromString(initialCuisine)?.let { add(it) }
            },
        )
    }
    var selectedDistrict by remember { mutableStateOf(initialDistrict) }
    var filteredResults by remember { mutableStateOf<List<Restaurant>>(emptyList()) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        allRestaurants = repository.getAll()
        loading = false
    }

    // Auto-focus the search field
    LaunchedEffect(Unit) {
        delay(200)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
            // Focus request may fail if composable is not yet laid out
        }
    }

    // Debounced filtering. Tag filter is OR semantics: a restaurant matches
    // if any of its tags is in the selection (so picking SICHUAN + HOTPOT
    // shows everything Sichuan-related plus everything hotpot-related).
    LaunchedEffect(query, selectedTags, selectedDistrict, allRestaurants) {
        delay(300)
        filteredResults = allRestaurants.filter { restaurant ->
            val matchesQuery = query.isBlank() ||
                restaurant.name.zh.contains(query, ignoreCase = true) ||
                restaurant.name.en.contains(query, ignoreCase = true) ||
                (restaurant.name.de?.contains(query, ignoreCase = true) == true)

            val matchesTags = selectedTags.isEmpty() ||
                restaurant.tags.any { it in selectedTags }

            val matchesDistrict = selectedDistrict == null || restaurant.address.district == selectedDistrict

            matchesQuery && matchesTags && matchesDistrict
        }
    }

    val districts = remember(allRestaurants) {
        allRestaurants.map { it.address.district }.distinct().sorted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(Res.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            // Tag chips (multi-select)
            TagChips(
                selected = selectedTags,
                onToggle = { tag ->
                    selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                },
                onClear = { selectedTags = emptySet() },
            )

            // District chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedDistrict == null,
                    onClick = { selectedDistrict = null },
                    label = { Text(stringResource(Res.string.all)) },
                )
                districts.forEach { district ->
                    FilterChip(
                        selected = selectedDistrict == district,
                        onClick = { selectedDistrict = district },
                        label = { Text(district) },
                    )
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (filteredResults.isEmpty() && !loading) {
                EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = stringResource(Res.string.no_results),
                    subtitle = "",
                )
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredResults, key = { it.id }) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        onClick = { onNavigateDetail(restaurant.id) },
                    )
                }
            }
        }
    }
}
