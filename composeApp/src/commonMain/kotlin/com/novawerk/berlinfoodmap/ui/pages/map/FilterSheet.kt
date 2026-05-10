package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.back
import berlinfoodmap.composeapp.generated.resources.filter_all
import berlinfoodmap.composeapp.generated.resources.filter_apply
import berlinfoodmap.composeapp.generated.resources.filter_apply_count
import berlinfoodmap.composeapp.generated.resources.filter_clear
import berlinfoodmap.composeapp.generated.resources.filter_favorites_only
import berlinfoodmap.composeapp.generated.resources.filter_favorites_subtitle
import berlinfoodmap.composeapp.generated.resources.filter_featured_only
import berlinfoodmap.composeapp.generated.resources.filter_featured_subtitle
import berlinfoodmap.composeapp.generated.resources.filter_open_now
import berlinfoodmap.composeapp.generated.resources.filter_open_now_subtitle
import berlinfoodmap.composeapp.generated.resources.filter_reset
import berlinfoodmap.composeapp.generated.resources.filter_title
import berlinfoodmap.composeapp.generated.resources.tag_family_format
import berlinfoodmap.composeapp.generated.resources.tag_family_regional
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.TagFamily
import com.novawerk.berlinfoodmap.domain.restaurant.family
import com.novawerk.berlinfoodmap.ui.components.tagDescription
import com.novawerk.berlinfoodmap.ui.components.tagDisplayName
import org.jetbrains.compose.resources.stringResource

/**
 * Two-level filter sheet with a single draft layer.
 *
 * Nothing the user touches takes effect until they tap "Apply" at the
 * bottom — the favorites toggle, cuisine selections, and format selections
 * all mutate a sheet-local draft. Sheet dismissal (swipe / scrim) discards
 * the draft. This avoids the "I tapped one chip and the whole map
 * rerendered" feeling, and lets the user batch their intent.
 *
 * The summary pane shows draft state (so the user sees what they've staged).
 * The picker panes operate on the same draft — tapping rows mutates the
 * parent draft directly, with the back arrow simply returning to summary.
 *
 * Within a family, multi-select is OR (any match passes). Across families,
 * AND. Per-row picker counts shrink to whatever the OTHER family's draft
 * already constrains.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(
    allRestaurants: List<Restaurant>,
    favorites: Set<String>,
    selectedCuisines: Set<Tag>,
    selectedFormats: Set<Tag>,
    favoritesOnly: Boolean,
    featuredOnly: Boolean,
    openNow: Boolean,
    onApply: (
        favoritesOnly: Boolean,
        featuredOnly: Boolean,
        openNow: Boolean,
        cuisines: Set<Tag>,
        formats: Set<Tag>,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var page by rememberSaveable { mutableStateOf(SheetPage.Summary) }

    // Single draft layer. Seeded from the live filter on entry; resets
    // when the live filter changes externally (e.g., another caller resets).
    var draftFavoritesOnly by remember(favoritesOnly) { mutableStateOf(favoritesOnly) }
    var draftFeaturedOnly by remember(featuredOnly) { mutableStateOf(featuredOnly) }
    var draftOpenNow by remember(openNow) { mutableStateOf(openNow) }
    var draftCuisines by remember(selectedCuisines) { mutableStateOf(selectedCuisines) }
    var draftFormats by remember(selectedFormats) { mutableStateOf(selectedFormats) }

    val featuredCount = remember(allRestaurants) { allRestaurants.count { it.featured } }
    val draftFilterCount = draftCuisines.size +
        draftFormats.size +
        (if (draftFavoritesOnly) 1 else 0) +
        (if (draftFeaturedOnly) 1 else 0) +
        (if (draftOpenNow) 1 else 0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Pin to plain surface — M3's default `surfaceContainerLow` would
        // pull a primary tint, which reads faintly purple against the
        // brand cream.
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut())
                }
            },
            label = "filter-sheet-page",
        ) { current ->
            when (current) {
                SheetPage.Summary -> SummaryPane(
                    favorites = favorites,
                    favoritesOnly = draftFavoritesOnly,
                    featuredOnly = draftFeaturedOnly,
                    openNow = draftOpenNow,
                    featuredCount = featuredCount,
                    selectedCuisines = draftCuisines,
                    selectedFormats = draftFormats,
                    draftFilterCount = draftFilterCount,
                    onFavoritesOnlyChange = { draftFavoritesOnly = it },
                    onFeaturedOnlyChange = { draftFeaturedOnly = it },
                    onOpenNowChange = { draftOpenNow = it },
                    onCuisineRowClick = { page = SheetPage.Cuisine },
                    onFormatRowClick = { page = SheetPage.Format },
                    onReset = {
                        draftFavoritesOnly = false
                        draftFeaturedOnly = false
                        draftOpenNow = false
                        draftCuisines = emptySet()
                        draftFormats = emptySet()
                    },
                    onApply = {
                        onApply(
                            draftFavoritesOnly,
                            draftFeaturedOnly,
                            draftOpenNow,
                            draftCuisines,
                            draftFormats,
                        )
                    },
                )
                SheetPage.Cuisine -> PickerPane(
                    title = stringResource(Res.string.tag_family_regional),
                    family = TagFamily.REGIONAL,
                    allRestaurants = allRestaurants,
                    favorites = favorites,
                    favoritesOnly = draftFavoritesOnly,
                    featuredOnly = draftFeaturedOnly,
                    selected = draftCuisines,
                    otherFamilySelection = draftFormats,
                    onSelectedChange = { draftCuisines = it },
                    onBack = { page = SheetPage.Summary },
                )
                SheetPage.Format -> PickerPane(
                    title = stringResource(Res.string.tag_family_format),
                    family = TagFamily.FORMAT,
                    allRestaurants = allRestaurants,
                    favorites = favorites,
                    favoritesOnly = draftFavoritesOnly,
                    featuredOnly = draftFeaturedOnly,
                    selected = draftFormats,
                    otherFamilySelection = draftCuisines,
                    onSelectedChange = { draftFormats = it },
                    onBack = { page = SheetPage.Summary },
                )
            }
        }
    }
}

private enum class SheetPage { Summary, Cuisine, Format }

@Composable
private fun SummaryPane(
    favorites: Set<String>,
    favoritesOnly: Boolean,
    featuredOnly: Boolean,
    openNow: Boolean,
    featuredCount: Int,
    selectedCuisines: Set<Tag>,
    selectedFormats: Set<Tag>,
    draftFilterCount: Int,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onFeaturedOnlyChange: (Boolean) -> Unit,
    onOpenNowChange: (Boolean) -> Unit,
    onCuisineRowClick: () -> Unit,
    onFormatRowClick: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
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

        Column(modifier = Modifier.fillMaxWidth()) {
            ToggleRow(
                icon = Icons.Filled.Favorite,
                title = stringResource(Res.string.filter_favorites_only),
                subtitle = stringResource(Res.string.filter_favorites_subtitle),
                checked = favoritesOnly,
                // The user can still toggle off when there are zero
                // favorites — disabling-on-empty matches the "stale draft"
                // case where the filter is on but the set has been emptied
                // elsewhere.
                enabled = favorites.isNotEmpty() || favoritesOnly,
                onChange = onFavoritesOnlyChange,
            )
            ToggleRow(
                icon = Icons.Filled.Star,
                title = stringResource(Res.string.filter_featured_only),
                subtitle = stringResource(Res.string.filter_featured_subtitle),
                checked = featuredOnly,
                enabled = featuredCount > 0 || featuredOnly,
                onChange = onFeaturedOnlyChange,
            )
            ToggleRow(
                icon = Icons.Filled.AccessTime,
                title = stringResource(Res.string.filter_open_now),
                subtitle = stringResource(Res.string.filter_open_now_subtitle),
                checked = openNow,
                enabled = true,
                onChange = onOpenNowChange,
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            SummaryRow(
                icon = Icons.Filled.Restaurant,
                label = stringResource(Res.string.tag_family_regional),
                selection = selectedCuisines,
                onClick = onCuisineRowClick,
            )
            SummaryRow(
                icon = Icons.Filled.RestaurantMenu,
                label = stringResource(Res.string.tag_family_format),
                selection = selectedFormats,
                onClick = onFormatRowClick,
            )
        }

        // Apply lives at the bottom of the sheet so the user can tap it
        // with one thumb after they've finished staging changes.
        // navigationBarsPadding keeps it clear of the system gesture bar.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp)
                .navigationBarsPadding(),
        ) {
            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = if (draftFilterCount == 0) {
                        stringResource(Res.string.filter_apply)
                    } else {
                        stringResource(Res.string.filter_apply_count, draftFilterCount)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    icon: ImageVector,
    label: String,
    selection: Set<Tag>,
    onClick: () -> Unit,
) {
    val active = selection.isNotEmpty()
    // Resolve display names via an inline forEach so the @Composable context
    // propagates — joinToString / map are not inline and reject @Composable
    // calls in their lambda.
    val names = mutableListOf<String>()
    selection.forEach { names.add(tagDisplayName(it)) }
    val subtitle = if (active) {
        names.joinToString(separator = "、")
    } else {
        stringResource(Res.string.filter_all)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun PickerPane(
    title: String,
    family: TagFamily,
    allRestaurants: List<Restaurant>,
    favorites: Set<String>,
    favoritesOnly: Boolean,
    featuredOnly: Boolean,
    selected: Set<Tag>,
    otherFamilySelection: Set<Tag>,
    onSelectedChange: (Set<Tag>) -> Unit,
    onBack: () -> Unit,
) {
    val tags = remember(family) { Tag.entries.filter { it.family() == family } }

    // Counts respect the other family's selection plus the boolean toggles,
    // so the number is what the user would actually see if they selected
    // JUST this tag. Within-family is OR, so a tag's count is independent
    // of other tags in the same family.
    val pool = remember(allRestaurants, favoritesOnly, favorites, featuredOnly) {
        allRestaurants.filter { r ->
            (!favoritesOnly || r.id in favorites) &&
                (!featuredOnly || r.featured)
        }
    }
    val counts = remember(pool, otherFamilySelection) {
        tags.associateWith { tag ->
            pool.count { r ->
                tag in r.tags && (
                    otherFamilySelection.isEmpty() ||
                        otherFamilySelection.any { it in r.tags }
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            // Clear empties this family's draft selection in place. The
            // user still has to tap Apply on the summary to commit.
            if (selected.isNotEmpty()) {
                TextButton(onClick = { onSelectedChange(emptySet()) }) {
                    Text(stringResource(Res.string.filter_clear))
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            tags.forEach { tag ->
                TagPickerRow(
                    title = tagDisplayName(tag),
                    subtitle = tagDescription(tag),
                    count = counts[tag] ?: 0,
                    selected = tag in selected,
                    onClick = {
                        onSelectedChange(
                            if (tag in selected) selected - tag else selected + tag,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TagPickerRow(
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
        SelectionIndicator(selected = selected)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
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

@Composable
private fun SelectionIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(percent = 50),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) {
                MaterialTheme.colorScheme.primary
            } else if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                color = if (checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
        )
    }
}
