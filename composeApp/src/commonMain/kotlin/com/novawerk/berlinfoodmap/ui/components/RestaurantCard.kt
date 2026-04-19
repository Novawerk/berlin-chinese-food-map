package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.restaurant.CuisineType
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.cuisine_sichuan
import berlinfoodmap.composeapp.generated.resources.cuisine_cantonese
import berlinfoodmap.composeapp.generated.resources.cuisine_hotpot
import berlinfoodmap.composeapp.generated.resources.cuisine_bbq
import berlinfoodmap.composeapp.generated.resources.cuisine_dim_sum
import berlinfoodmap.composeapp.generated.resources.cuisine_noodles
import berlinfoodmap.composeapp.generated.resources.cuisine_general
import berlinfoodmap.composeapp.generated.resources.cuisine_other

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = restaurant.name.zh,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = restaurant.name.en,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = restaurant.address.district,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = " \u00B7 ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = cuisineDisplayName(restaurant.cuisineType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (restaurant.priceRange != null) {
                    Text(
                        text = " \u00B7 ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = restaurant.priceRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${restaurant.viewCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(16.dp))
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${restaurant.visitCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun cuisineDisplayName(cuisineType: CuisineType): String = when (cuisineType) {
    CuisineType.SICHUAN -> stringResource(Res.string.cuisine_sichuan)
    CuisineType.CANTONESE -> stringResource(Res.string.cuisine_cantonese)
    CuisineType.HOTPOT -> stringResource(Res.string.cuisine_hotpot)
    CuisineType.BBQ -> stringResource(Res.string.cuisine_bbq)
    CuisineType.DIM_SUM -> stringResource(Res.string.cuisine_dim_sum)
    CuisineType.NOODLES -> stringResource(Res.string.cuisine_noodles)
    CuisineType.GENERAL -> stringResource(Res.string.cuisine_general)
    CuisineType.OTHER -> stringResource(Res.string.cuisine_other)
}
