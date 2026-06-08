package com.novawerk.berlinfoodmap.ui.pages.favorites

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.app_name
import berlinfoodmap.composeapp.generated.resources.favorites_empty_cta
import berlinfoodmap.composeapp.generated.resources.favorites_empty_subtitle
import berlinfoodmap.composeapp.generated.resources.favorites_empty_title
import berlinfoodmap.composeapp.generated.resources.favorites_title
import berlinfoodmap.composeapp.generated.resources.pinwo_wordmark
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.ui.components.AppBottomSheet
import com.novawerk.berlinfoodmap.ui.components.RestaurantCard
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom sheet listing the user's favorited restaurants. Triggered from the
 * new bottom-nav heart icon AND the special Favorites card on the search
 * grid. Tapping a row opens the restaurant detail sheet.
 */
@Composable
fun FavoritesSheet(
    favorites: List<Restaurant>,
    onRestaurantClick: (String) -> Unit,
    onBrowseMap: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        item {
            Text(
                text = stringResource(Res.string.favorites_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        if (favorites.isEmpty()) {
            item { EmptyState(onBrowseMap = onBrowseMap) }
        } else {
            items(favorites, key = { it.id }) { restaurant ->
                Box(modifier = Modifier.padding(bottom = 10.dp)) {
                    RestaurantCard(
                        restaurant = restaurant,
                        onClick = { onRestaurantClick(restaurant.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onBrowseMap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.pinwo_wordmark),
                contentDescription = stringResource(Res.string.app_name),
                modifier = Modifier.size(width = 120.dp, height = 36.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.favorites_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.favorites_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onBrowseMap) {
                Text(stringResource(Res.string.favorites_empty_cta))
            }
        }
    }
}
