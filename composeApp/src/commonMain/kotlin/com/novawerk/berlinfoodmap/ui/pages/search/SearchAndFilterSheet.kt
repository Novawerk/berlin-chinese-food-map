package com.novawerk.berlinfoodmap.ui.pages.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.districts_action
import berlinfoodmap.composeapp.generated.resources.filter_all
import berlinfoodmap.composeapp.generated.resources.filter_favorites_only
import berlinfoodmap.composeapp.generated.resources.filter_featured_only
import berlinfoodmap.composeapp.generated.resources.filter_open_now
import berlinfoodmap.composeapp.generated.resources.filter_reset
import berlinfoodmap.composeapp.generated.resources.no_results
import berlinfoodmap.composeapp.generated.resources.search_and_filter_title
import berlinfoodmap.composeapp.generated.resources.search_cancel
import berlinfoodmap.composeapp.generated.resources.search_hint
import berlinfoodmap.composeapp.generated.resources.search_prompt_subtitle
import berlinfoodmap.composeapp.generated.resources.tag_family_format
import berlinfoodmap.composeapp.generated.resources.tag_family_regional
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.TagFamily
import com.novawerk.berlinfoodmap.domain.restaurant.family
import com.novawerk.berlinfoodmap.ui.components.EmptyState
import com.novawerk.berlinfoodmap.ui.components.RestaurantCard
import com.novawerk.berlinfoodmap.ui.components.tagDescription
import com.novawerk.berlinfoodmap.ui.components.tagDisplayName
import com.novawerk.berlinfoodmap.ui.components.tagPhoto
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val MIN_QUERY_LENGTH = 2

/**
 * Unified search + filter bottom sheet. Replaces the old top SearchBar and
 * the bottom-FAB FilterSheet — both surfaces now live in one place reachable
 * via the bottom-nav search icon.
 *
 * Layout:
 *  - Sticky header: title + reset (clears all live filters)
 *  - Toggle chip row: favorites / featured / open-now (live toggles)
 *  - Sticky search field
 *  - Body: tag picker (cuisine + format cards) when query is blank,
 *    or search results list when the user has typed ≥ [MIN_QUERY_LENGTH] chars.
 *
 * Tag taps mutate the [MapViewModel] live (no draft/apply layer) — the user
 * said they want immediate feedback, and the map redraws behind the sheet so
 * dismissing the sheet shows the filtered map without any extra step.
 *
 * Search matches name in ZH/EN/DE plus tag aliases (see [matchTagsForQuery])
 * and ignores the live filter so the user can find anything they typed even
 * if it would be filtered out on the map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterSheet(
    allRestaurants: List<Restaurant>,
    favorites: Set<String>,
    selectedCuisines: Set<Tag>,
    selectedFormats: Set<Tag>,
    favoritesOnly: Boolean,
    featuredOnly: Boolean,
    openNow: Boolean,
    onCuisinesChange: (Set<Tag>) -> Unit,
    onFormatsChange: (Set<Tag>) -> Unit,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onFeaturedOnlyChange: (Boolean) -> Unit,
    onOpenNowChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
    onBrowseDistricts: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Pin to plain surface — M3's default surfaceContainerLow pulls a
        // primary tint that reads faintly purple against the brand cream.
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SheetBody(
            allRestaurants = allRestaurants,
            favorites = favorites,
            selectedCuisines = selectedCuisines,
            selectedFormats = selectedFormats,
            favoritesOnly = favoritesOnly,
            featuredOnly = featuredOnly,
            openNow = openNow,
            onCuisinesChange = onCuisinesChange,
            onFormatsChange = onFormatsChange,
            onFavoritesOnlyChange = onFavoritesOnlyChange,
            onFeaturedOnlyChange = onFeaturedOnlyChange,
            onOpenNowChange = onOpenNowChange,
            onReset = onReset,
            onRestaurantClick = { restaurant ->
                onRestaurantClick(restaurant)
            },
            onBrowseDistricts = onBrowseDistricts,
            onTagPicked = onDismiss,
            sheetState = sheetState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetBody(
    allRestaurants: List<Restaurant>,
    favorites: Set<String>,
    selectedCuisines: Set<Tag>,
    selectedFormats: Set<Tag>,
    favoritesOnly: Boolean,
    featuredOnly: Boolean,
    openNow: Boolean,
    onCuisinesChange: (Set<Tag>) -> Unit,
    onFormatsChange: (Set<Tag>) -> Unit,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onFeaturedOnlyChange: (Boolean) -> Unit,
    onOpenNowChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
    onBrowseDistricts: () -> Unit,
    onTagPicked: () -> Unit,
    sheetState: SheetState,
) {
    var query by remember { mutableStateOf("") }
    val trimmed by remember(query) { derivedStateOf { query.trim() } }
    val hasQuery by remember(trimmed) {
        derivedStateOf { trimmed.length >= MIN_QUERY_LENGTH }
    }
    // Apple Music-style focus mode. Once the search field gains focus the
    // chrome above it (title, toggles, tag grid) collapses to leave only the
    // search bar + Cancel + results — keeping typing context clean. Cancel
    // clears the query and unfocuses, restoring the picker.
    var searchFocused by remember { mutableStateOf(false) }
    val searchActive by remember(searchFocused, hasQuery) {
        derivedStateOf { searchFocused || hasQuery }
    }
    val keyboard = LocalSoftwareKeyboardController.current

    var results by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    LaunchedEffect(trimmed, hasQuery, allRestaurants) {
        if (!hasQuery) {
            results = emptyList()
            return@LaunchedEffect
        }
        // 200ms debounce avoids re-ranking the full list on every keystroke.
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

    val activeCount = selectedCuisines.size +
        selectedFormats.size +
        (if (favoritesOnly) 1 else 0) +
        (if (featuredOnly) 1 else 0) +
        (if (openNow) 1 else 0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        if (!searchActive) {
            // Sticky header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.search_and_filter_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                if (activeCount > 0) {
                    TextButton(onClick = onReset) {
                        Text(stringResource(Res.string.filter_reset))
                    }
                }
            }

            // Quick-toggle row: scrollable to fit on narrow screens / when text grows.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToggleChip(
                    selected = favoritesOnly,
                    enabled = favorites.isNotEmpty() || favoritesOnly,
                    label = stringResource(Res.string.filter_favorites_only),
                    icon = Icons.Filled.Favorite,
                    onClick = { onFavoritesOnlyChange(!favoritesOnly) },
                )
                ToggleChip(
                    selected = featuredOnly,
                    enabled = true,
                    label = stringResource(Res.string.filter_featured_only),
                    icon = Icons.Filled.Star,
                    onClick = { onFeaturedOnlyChange(!featuredOnly) },
                )
                ToggleChip(
                    selected = openNow,
                    enabled = true,
                    label = stringResource(Res.string.filter_open_now),
                    icon = Icons.Filled.AccessTime,
                    onClick = { onOpenNowChange(!openNow) },
                )
                // District browsing lives here as a navigational shortcut — same
                // chip row so the user sees it as a peer to the toggles.
                AssistChip(
                    onClick = onBrowseDistricts,
                    label = { Text(stringResource(Res.string.districts_action)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
        }

        // Search field row. In picker mode, just the field; in search-active
        // mode, the field shares the row with a Cancel button that mirrors
        // Apple Music's behavior — drop the query, blur, return to picker.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { searchFocused = it.isFocused },
                placeholder = { Text(stringResource(Res.string.search_hint)) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* live results */ }),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            if (searchActive) {
                Spacer(Modifier.size(8.dp))
                TextButton(
                    onClick = {
                        query = ""
                        keyboard?.hide()
                        // Field stays in composition so we can't blur it
                        // directly — clearing focus state + query is enough
                        // for the searchActive predicate to flip back.
                        searchFocused = false
                    },
                ) {
                    Text(stringResource(Res.string.search_cancel))
                }
            }
        }

        // Body: tag picker OR search results.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .imePadding(),
        ) {
            if (searchActive) {
                ResultsList(
                    results = results,
                    onRestaurantClick = onRestaurantClick,
                )
            } else {
                TagPickerList(
                    allRestaurants = allRestaurants,
                    favorites = favorites,
                    favoritesOnly = favoritesOnly,
                    featuredOnly = featuredOnly,
                    selectedCuisines = selectedCuisines,
                    selectedFormats = selectedFormats,
                    onCuisinesChange = { tags ->
                        onCuisinesChange(tags)
                        // Selecting a tag is a one-shot pick — dismiss the
                        // sheet so the user immediately sees the filtered
                        // map. Deselects don't dismiss (handled by caller
                        // passing the same lambda; the sheet just closes
                        // on every call, which matches the requirement).
                        onTagPicked()
                    },
                    onFormatsChange = { tags ->
                        onFormatsChange(tags)
                        onTagPicked()
                    },
                )
            }
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
        state = rememberLazyListState(),
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

@Composable
private fun TagPickerList(
    allRestaurants: List<Restaurant>,
    favorites: Set<String>,
    favoritesOnly: Boolean,
    featuredOnly: Boolean,
    selectedCuisines: Set<Tag>,
    selectedFormats: Set<Tag>,
    onCuisinesChange: (Set<Tag>) -> Unit,
    onFormatsChange: (Set<Tag>) -> Unit,
) {
    val cuisineTags = remember { Tag.entries.filter { it.family() == TagFamily.REGIONAL } }
    val formatTags = remember { Tag.entries.filter { it.family() == TagFamily.FORMAT } }

    // Restaurant pool that respects the boolean toggles; tag counts shrink
    // to whatever the OTHER family's selection already constrains so the
    // numbers reflect what the user would actually see.
    val pool = remember(allRestaurants, favoritesOnly, favorites, featuredOnly) {
        allRestaurants.filter { r ->
            (!favoritesOnly || r.id in favorites) &&
                (!featuredOnly || r.featured)
        }
    }
    val cuisineCounts = remember(pool, selectedFormats) {
        cuisineTags.associateWith { tag ->
            pool.count { r ->
                tag in r.tags && (
                    selectedFormats.isEmpty() ||
                        selectedFormats.any { it in r.tags }
                )
            }
        }
    }
    val formatCounts = remember(pool, selectedCuisines) {
        formatTags.associateWith { tag ->
            pool.count { r ->
                tag in r.tags && (
                    selectedCuisines.isEmpty() ||
                        selectedCuisines.any { it in r.tags }
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("cuisine-header") {
            SectionHeader(stringResource(Res.string.tag_family_regional))
        }
        items(cuisineTags.chunked(2), key = { it.joinToString { tag -> tag.name } }) { rowTags ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowTags.forEach { tag ->
                    TagPhotoCard(
                        modifier = Modifier.weight(1f),
                        tag = tag,
                        count = cuisineCounts[tag] ?: 0,
                        selected = tag in selectedCuisines,
                        // Single-select per family — tap a tag and it becomes
                        // the sole filter for that family. Tap the same tag
                        // again to clear. Avoids the multi-select + confirm
                        // dance the user explicitly rejected.
                        onClick = {
                            onCuisinesChange(
                                if (tag in selectedCuisines) emptySet() else setOf(tag),
                            )
                        },
                    )
                }
                if (rowTags.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item("format-header") {
            SectionHeader(stringResource(Res.string.tag_family_format))
        }
        items(formatTags.chunked(2), key = { it.joinToString { tag -> tag.name } }) { rowTags ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowTags.forEach { tag ->
                    TagPhotoCard(
                        modifier = Modifier.weight(1f),
                        tag = tag,
                        count = formatCounts[tag] ?: 0,
                        selected = tag in selectedFormats,
                        onClick = {
                            onFormatsChange(
                                if (tag in selectedFormats) emptySet() else setOf(tag),
                            )
                        },
                    )
                }
                if (rowTags.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item("bottom-spacer") { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToggleChip(
    selected: Boolean,
    enabled: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}

@Composable
private fun TagPhotoCard(
    modifier: Modifier,
    tag: Tag,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cornerShape = RoundedCornerShape(14.dp)
    val emptyCount = count == 0

    Box(
        modifier = modifier
            .aspectRatio(2.45f)
            .clip(cornerShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = cornerShape,
                    )
                } else {
                    Modifier
                }
            ),
    ) {
        Image(
            painter = painterResource(tagPhoto(tag)),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (emptyCount && !selected) 0.55f else 1f),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.05f),
                        ),
                    ),
                ),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp, top = 10.dp, end = 36.dp, bottom = 10.dp),
        ) {
            Text(
                text = tagDisplayName(tag),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
            )
            val subtitle = tagDescription(tag)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(percent = 50),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else if (count > 0) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(percent = 50),
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

