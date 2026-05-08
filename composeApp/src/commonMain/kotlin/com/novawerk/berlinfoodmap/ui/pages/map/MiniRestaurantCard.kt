package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.Image
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.ui.components.cardTags
import com.novawerk.berlinfoodmap.ui.components.rememberIsCurrentlyClosed
import com.novawerk.berlinfoodmap.ui.components.tagDisplayName

/**
 * Pill-shaped marker label.
 *
 *  - [coverImage] non-null → wrap with `Image.asPainter(ctx)` and render.
 *    The wrap is a cheap struct adapter (see `coil3.compose.ImagePainter`)
 *    — no decoding, no async work; the bitmap is already in memory.
 *  - [coverImage] null → fallback Material restaurant icon. Same fallback
 *    is used for both "load failed" and "no cover URL" — visually
 *    identical, semantically interchangeable for the user.
 *
 * Subtitle line shows the restaurant's [cardTags] joined with `·`, with a
 * star prefix for editorially-featured restaurants.
 */
@Composable
internal fun MiniRestaurantCard(
    restaurant: Restaurant,
    coverImage: Image?,
    isFavorite: Boolean = false,
) {
    val isClosed = rememberIsCurrentlyClosed(restaurant.googleData?.periods.orEmpty())
    val nameColor = if (isClosed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.onSurface
    val secondaryColor = if (isClosed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            // 0.75 alpha on the fill keeps the bubble readable while letting
            // the underlying map (roads, neighbouring labels, transit icons)
            // show through, so a card never fully occludes an icon next to
            // it. Foreground content (cover, name, tags) stays fully opaque.
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                MarkerBubbleShape,
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                MarkerBubbleShape,
            )
            // Bottom padding leaves the tail area clear so content stays above
            // the pointer triangle (the bubble itself extends `MARKER_TAIL_HEIGHT`
            // below the body).
            .padding(
                start = 6.dp,
                end = 10.dp,
                top = 6.dp,
                bottom = 6.dp + MARKER_TAIL_HEIGHT,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isClosed -> Icon(
                        imageVector = Icons.Filled.AirlineSeatFlat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    coverImage != null -> {
                        val ctx = LocalPlatformContext.current
                        val painter = remember(coverImage, ctx) { coverImage.asPainter(ctx) }
                        Image(
                            painter = painter,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (restaurant.featured) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isClosed) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    if (isFavorite) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isClosed) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        text = restaurant.name.zh,
                        style = MaterialTheme.typography.labelMedium,
                        color = nameColor,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                val displayTags = restaurant.cardTags()
                if (displayTags.isNotEmpty()) {
                    val labels = displayTags.map { tagDisplayName(it) }
                    Text(
                        text = labels.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val MARKER_CORNER_RADIUS = 10.dp
private val MARKER_TAIL_WIDTH = 10.dp
private val MARKER_TAIL_HEIGHT = 6.dp

/**
 * Speech-bubble outline for restaurant markers — rounded rectangle body with
 * a downward-pointing triangular tail centered horizontally. The bitmap's
 * `Marker(anchor = (0.5f, 1f))` puts the tail tip exactly on the
 * restaurant's lat/lng, so the bubble visually points to the road.
 *
 * Body and tail are merged via `Path.op(Union)` into one closed path so the
 * 1dp border traces the whole outline (body + tail) without an interior seam.
 */
private val MarkerBubbleShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = with(density) {
        val cr = MARKER_CORNER_RADIUS.toPx()
        val tw = MARKER_TAIL_WIDTH.toPx()
        val th = MARKER_TAIL_HEIGHT.toPx()
        val bodyBottom = size.height - th
        val centerX = size.width / 2f
        val tailLeft = (centerX - tw / 2f).coerceAtLeast(cr)
        val tailRight = (centerX + tw / 2f).coerceAtMost(size.width - cr)
        val tipX = (tailLeft + tailRight) / 2f

        val body = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, bodyBottom),
                    cornerRadius = CornerRadius(cr, cr),
                ),
            )
        }
        val tail = Path().apply {
            moveTo(tailLeft, bodyBottom)
            lineTo(tipX, size.height)
            lineTo(tailRight, bodyBottom)
            close()
        }
        Outline.Generic(Path().apply { op(body, tail, PathOperation.Union) })
    }
}
