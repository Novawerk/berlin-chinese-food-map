package com.novawerk.berlinfoodmap.ui.pages.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.back
import berlinfoodmap.composeapp.generated.resources.no_results
import berlinfoodmap.composeapp.generated.resources.search_hint
import berlinfoodmap.composeapp.generated.resources.search_prompt_subtitle
import berlinfoodmap.composeapp.generated.resources.search_prompt_title
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.ui.components.EmptyState
import com.novawerk.berlinfoodmap.ui.components.RestaurantCard
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** Minimum trimmed query length before any results are shown. */
private const val MIN_QUERY_LENGTH = 2

/**
 * M3 [SearchBar] wired to the restaurant repository. Lives at the top of the
 * map: collapsed it's a pill, tapping expands it in place into a full-screen
 * search overlay (no separate route). Matches restaurant names across
 * ZH / EN / DE *and* tag aliases (cuisine + format, e.g. "火锅", "川菜",
 * "hotpot" — see [matchTagsForQuery]). Name hits rank above tag-only hits.
 * Active map filters are ignored.
 *
 * Tapping a result fires [onRestaurantClick] and collapses the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantSearchBar(
    repository: RestaurantRepository,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var allRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var results by remember { mutableStateOf<List<Restaurant>>(emptyList()) }

    LaunchedEffect(Unit) {
        allRestaurants = repository.getAll()
        loading = false
    }

    val trimmed by remember(query) { derivedStateOf { query.trim() } }
    val hasQuery by remember(trimmed) { derivedStateOf { trimmed.length >= MIN_QUERY_LENGTH } }

    LaunchedEffect(trimmed, hasQuery, allRestaurants) {
        if (!hasQuery) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(200)
        val matchedTags = matchTagsForQuery(trimmed)
        val nameHits = mutableListOf<Restaurant>()
        val tagOnlyHits = mutableListOf<Restaurant>()
        for (restaurant in allRestaurants) {
            val nameHit = restaurant.name.zh.contains(trimmed, ignoreCase = true) ||
                restaurant.name.en.contains(trimmed, ignoreCase = true) ||
                (restaurant.name.de?.contains(trimmed, ignoreCase = true) == true)
            when {
                nameHit -> nameHits += restaurant
                matchedTags.isNotEmpty() && restaurant.tags.any { it in matchedTags } ->
                    tagOnlyHits += restaurant
            }
        }
        results = nameHits + tagOnlyHits
    }

    val collapse = {
        expanded = false
        query = ""
    }

    SearchBar(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { open ->
            expanded = open
            if (!open) query = ""
        },
        // Default colors mix primary tint into the surface ("tonal elevation"),
        // which renders as light purple in the app's warm-cream theme. Pin the
        // container to plain surface and zero out tonal elevation so both the
        // collapsed pill and the expanded sheet read as the same neutral cream.
        // shadowElevation gives the collapsed pill a real drop shadow against
        // the map without re-introducing the tint.
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        tonalElevation = 0.dp,
        shadowElevation = 4.dp,
        // The Scaffold already pads NavHost for the status bar; SearchBar adds
        // its own status-bar inset by default, so without zeroing it out we'd
        // double the top padding when expanded.
        windowInsets = WindowInsets(0),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = { /* live results — no explicit submit */ },
                expanded = expanded,
                onExpandedChange = { open ->
                    expanded = open
                    if (!open) query = ""
                },
                placeholder = { Text(stringResource(Res.string.search_hint)) },
                leadingIcon = {
                    if (expanded) {
                        IconButton(onClick = collapse) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.back),
                            )
                        }
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                },
                trailingIcon = if (expanded && query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }
                } else null,
            )
        },
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            !hasQuery -> EmptyState(
                icon = Icons.Filled.Search,
                title = stringResource(Res.string.search_prompt_title),
                subtitle = stringResource(Res.string.search_prompt_subtitle),
            )
            results.isEmpty() -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = stringResource(Res.string.no_results),
                subtitle = "",
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results, key = { it.id }) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        onClick = {
                            collapse()
                            onRestaurantClick(restaurant.id)
                        },
                    )
                }
            }
        }
    }
}
