package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.novawerk.berlinfoodmap.domain.common.preferred
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.previewImageUrl
import com.novawerk.berlinfoodmap.ui.components.cardTags
import com.novawerk.berlinfoodmap.ui.components.rememberIsCurrentlyClosed
import com.novawerk.berlinfoodmap.ui.components.tagDisplayName
import com.novawerk.berlinfoodmap.ui.locale.LocalAppLocale

// Cover decode size for the 44 dp NearbyCard thumbnail (rendered side × 4
// for xxxhdpi). Saves ~95% of the bitmap memory vs decoding the source
// 1600 px JPEG when 30+ NearbyCards scroll past in the viewport row.
private const val NEARBY_COVER_PX = 192

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NearbyCard(
    restaurant: Restaurant,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        val isClosed = rememberIsCurrentlyClosed(restaurant.googleData?.periods.orEmpty())
        // Closed venues fade their text — the swapped-in moon thumbnail does
        // most of the visual work, the dimmer text reinforces it.
        val nameColor = if (isClosed) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        val secondaryColor = if (isClosed) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        val tagColor = if (isClosed) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.primary
        }

        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val coverUrl = restaurant.previewImageUrl()
            val ctx = LocalPlatformContext.current
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isClosed -> Icon(
                        Icons.Filled.AirlineSeatFlat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    coverUrl != null -> AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(coverUrl)
                            .size(NEARBY_COVER_PX, NEARBY_COVER_PX)
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
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                // In English mode show the English name only (falling back to
                // zh when the English name is blank). In Chinese mode keep the
                // bilingual stack: zh as the primary line, en as secondary.
                val locale = LocalAppLocale.current
                val isZh = locale.startsWith("zh", ignoreCase = true)
                val primaryName = if (isZh) restaurant.name.zh else restaurant.name.preferred(locale)
                val secondaryName = restaurant.name.en
                    .takeIf { isZh && it.isNotBlank() && it != primaryName }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = primaryName,
                        style = MaterialTheme.typography.titleSmall,
                        color = nameColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isFavorite) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isClosed) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                // Always render the secondary slot — empty when there's no
                // second name (English mode, or zh == en) — so the card height
                // stays constant whether one or two name lines are shown.
                // minLines = 1 reserves the line height even when blank.
                Text(
                    text = secondaryName.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor,
                    maxLines = 1,
                    minLines = 1,
                )
                val displayTags = restaurant.cardTags()
                if (displayTags.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    val tagLine = displayTags.map { tagDisplayName(it) }.joinToString(" · ")
                    Text(
                        text = tagLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = tagColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
