package com.novawerk.berlinfoodmap.ui.pages.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.favorites_title
import berlinfoodmap.composeapp.generated.resources.no_results
import berlinfoodmap.composeapp.generated.resources.search_and_filter_title
import berlinfoodmap.composeapp.generated.resources.search_cancel
import berlinfoodmap.composeapp.generated.resources.search_hint_long
import berlinfoodmap.composeapp.generated.resources.search_prompt_subtitle
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.TagFamily
import com.novawerk.berlinfoodmap.domain.restaurant.family
import com.novawerk.berlinfoodmap.ui.components.EmptyState
import com.novawerk.berlinfoodmap.ui.components.RestaurantCard
import com.novawerk.berlinfoodmap.ui.components.tagDisplayName
import com.novawerk.berlinfoodmap.ui.components.tagPhoto
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val MIN_QUERY_LENGTH = 2

/**
 * Search & category sheet (May-24 redesign).
 *
 * Layout:
 *  - 2-column grid of full-bleed photo cards: first cell is a special
 *    "Favorites" red card with a heart icon; the remaining 22 cells are
 *    the cuisine/format tags, mixed without family grouping (matches the
 *    Sketch mockup).
 *  - Sticky bottom bar: text input ("今天想吃点儿什么？") + map shortcut
 *    + favorites shortcut.
 *  - Typing ≥ 2 chars switches the body to a search-results list; the
 *    Cancel button restores the grid.
 *
 * Single-tap a tag card → applies it as the sole filter (one regional or
 * one format) and dismisses the sheet. Single-tap the Favorites card →
 * opens the dedicated favorites list (handled via [onOpenFavorites]).
 *
 * The district picker, "editor's pick" / "open now" / "favorites" toggle
 * chips, and the multi-tag selection UI have all been removed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterSheet(
    allRestaurants: List<Restaurant>,
    favorites: Set<String>,
    onPickTag: (Tag) -> Unit,
    onShowOnMap: () -> Unit,
    onShowFavoritesOnMap: () -> Unit,
    onOpenFavorites: () -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SheetBody(
            allRestaurants = allRestaurants,
            favorites = favorites,
            onPickTag = { tag ->
                onPickTag(tag)
                onDismiss()
            },
            onOpenFavorites = onOpenFavorites,
            onShowOnMap = {
                onShowOnMap()
                onDismiss()
            },
            onShowFavoritesOnMap = {
                onShowFavoritesOnMap()
                onDismiss()
            },
            onRestaurantClick = onRestaurantClick,
        )
    }
}

@Composable
private fun SheetBody(
    allRestaurants: List<Restaurant>,
    favorites: Set<String>,
    onPickTag: (Tag) -> Unit,
    onOpenFavorites: () -> Unit,
    onShowOnMap: () -> Unit,
    onShowFavoritesOnMap: () -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val trimmed by remember(query) { derivedStateOf { query.trim() } }
    val hasQuery by remember(trimmed) {
        derivedStateOf { trimmed.length >= MIN_QUERY_LENGTH }
    }
    val keyboard = LocalSoftwareKeyboardController.current

    var results by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    LaunchedEffect(trimmed, hasQuery, allRestaurants) {
        if (!hasQuery) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(200) // debounce typing
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        // Title row (only when not actively searching).
        if (!hasQuery) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.search_and_filter_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Body: grid (default) or search results (when query is active)
        Box(modifier = Modifier.weight(1f, fill = true).fillMaxWidth()) {
            if (hasQuery) {
                ResultsList(
                    results = results,
                    onRestaurantClick = onRestaurantClick,
                )
            } else {
                CategoryGrid(
                    allRestaurants = allRestaurants,
                    favorites = favorites,
                    onPickTag = onPickTag,
                    onOpenFavorites = onOpenFavorites,
                )
            }
        }

        // Sticky bottom bar — anchors the search input + map / favorites
        // shortcuts to the bottom of the sheet so they're always reachable
        // without dismissing.
        BottomActionBar(
            query = query,
            onQueryChange = { query = it },
            onClearQuery = {
                query = ""
                keyboard?.hide()
            },
            onShowOnMap = onShowOnMap,
            onShowFavoritesOnMap = onShowFavoritesOnMap,
        )
    }
}

@Composable
private fun CategoryGrid(
    allRestaurants: List<Restaurant>,
    favorites: Set<String>,
    onPickTag: (Tag) -> Unit,
    onOpenFavorites: () -> Unit,
) {
    val tags = remember {
        // Tag.entries preserves declaration order: regional first, then format.
        // The grid mixes them as-is per the Sketch mockup (no headers).
        Tag.entries.toList()
    }
    val tagCounts = remember(allRestaurants, tags) {
        tags.associateWith { tag -> allRestaurants.count { tag in it.tags } }
    }
    val favoriteCount = favorites.size

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Special Favorites card first.
        item(key = "__favorites__") {
            FavoritesCard(count = favoriteCount, onClick = onOpenFavorites)
        }

        items(tags, key = { it.name }) { tag ->
            TagPhotoCard(
                tag = tag,
                count = tagCounts[tag] ?: 0,
                onClick = { onPickTag(tag) },
            )
        }
    }
}

@Composable
private fun FavoritesCard(count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.0f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.favorites_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            if (count > 0) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
                .size(28.dp),
        )
    }
}

@Composable
private fun TagPhotoCard(
    tag: Tag,
    count: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.0f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(tagPhoto(tag)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.0f),
                            Color.Black.copy(alpha = 0.55f),
                        ),
                    ),
                ),
        )
        Text(
            text = tagDisplayName(tag),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 10.dp, end = 36.dp),
        )
        if (count > 0) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(percent = 50),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun ResultsList(
    results: List<Restaurant>,
    onRestaurantClick: (Restaurant) -> Unit,
) {
    if (results.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.SearchOff,
            title = stringResource(Res.string.no_results),
            subtitle = stringResource(Res.string.search_prompt_subtitle),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(results, key = { it.id }) { restaurant ->
            RestaurantCard(
                restaurant = restaurant,
                onClick = { onRestaurantClick(restaurant) },
            )
        }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomActionBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onShowOnMap: () -> Unit,
    onShowFavoritesOnMap: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(Res.string.search_hint_long)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(Res.string.search_cancel),
                            )
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                shape = RoundedCornerShape(percent = 50),
            )
            ShortcutButton(
                icon = Icons.Filled.Map,
                onClick = onShowOnMap,
            )
            ShortcutButton(
                icon = Icons.Filled.Favorite,
                onClick = onShowFavoritesOnMap,
            )
        }
    }
}

@Composable
private fun ShortcutButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
