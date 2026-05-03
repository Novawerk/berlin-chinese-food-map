package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.previewImageUrl

// Cover decode size for the 80 dp list thumbnail (rendered side × 4 for
// xxxhdpi). Saves bitmap memory vs decoding the source 1600 px JPEG.
private const val LIST_COVER_PX = 320

/**
 * Full-width list card — the canonical "restaurant in a vertical list" UI.
 * Mirrors the cover-thumbnail look of NearbyCard so search results feel
 * continuous with the rest of the app: cover image, ZH primary name,
 * EN secondary, regional/format tag line, district + price line.
 *
 * Closed restaurants render with a moon thumbnail and faded text — same
 * convention as map cards.
 */
@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isClosed = rememberIsCurrentlyClosed(restaurant.googleData?.periods.orEmpty())
    val nameColor = if (isClosed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.onSurface
    val secondaryColor = if (isClosed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.onSurfaceVariant
    val tagColor = if (isClosed) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverThumb(restaurant = restaurant, isClosed = isClosed)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = restaurant.name.zh,
                        style = MaterialTheme.typography.titleMedium,
                        color = nameColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (restaurant.featured) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = tagColor,
                        )
                    }
                }
                Text(
                    text = restaurant.name.en,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor,
                    maxLines = 1,
                )
                val displayTags = restaurant.cardTags()
                if (displayTags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    val labels = displayTags.map { tagDisplayName(it) }
                    Text(
                        text = labels.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = tagColor,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = secondaryColor,
                    )
                    Text(
                        text = restaurant.address.district,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                        maxLines = 1,
                    )
                    if (!restaurant.priceRange.isNullOrBlank()) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor,
                        )
                        Text(
                            text = restaurant.priceRange!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverThumb(
    restaurant: Restaurant,
    isClosed: Boolean,
) {
    val coverUrl = restaurant.previewImageUrl()
    val ctx = LocalPlatformContext.current
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isClosed -> Icon(
                Icons.Filled.AirlineSeatFlat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            coverUrl != null -> AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(coverUrl)
                    .size(LIST_COVER_PX, LIST_COVER_PX)
                    .crossfade(durationMillis = 200)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Icon(
                Icons.Filled.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
