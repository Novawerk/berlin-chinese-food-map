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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
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
 * Favorite / featured state is shown as a floating badge overhanging
 * the pill's top-left corner (heart for favorite, star for featured).
 * Favorite wins when both are true — the user's saved state is more
 * informative than editorial curation. Subtitle line below the name
 * shows the restaurant's [cardTags] joined with `·`.
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

    // Wrapper Box reserves space around the pill so the floating badge can
    // overhang the top-left corner without being clipped by the rasterised
    // bitmap. Symmetric horizontal padding (start = end = OVERHANG) keeps
    // the pill — and therefore its tail tip — horizontally centred in the
    // bitmap so `Marker(anchor = (0.5, 1))` still lands on the lat/lng.
    Box(
        modifier = Modifier.padding(
            start = MARKER_BADGE_OVERHANG,
            end = MARKER_BADGE_OVERHANG,
            top = MARKER_BADGE_OVERHANG,
        ),
    ) {
        Box(
            modifier = Modifier
                // Solid fill — the previous 0.75-alpha treatment let map labels
                // bleed through and read as visual noise. Now that overlap is
                // resolved by clustering (AABB-based, see `clusterFromProjected`)
                // rather than by translucency, the pill renders fully opaque.
                .background(
                    MaterialTheme.colorScheme.surface,
                    MarkerBubbleShape,
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
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
                // Cap the text column width so long zh names + tag strings can't
                // grow the pill arbitrarily — the AABB overlap check downstream
                // is calibrated against this max. Names that exceed it ellipsize.
                Column(modifier = Modifier.widthIn(max = MARKER_TEXT_COLUMN_MAX_WIDTH)) {
                    Text(
                        text = restaurant.name.zh,
                        style = MaterialTheme.typography.labelMedium,
                        color = nameColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val displayTags = restaurant.cardTags()
                    if (displayTags.isNotEmpty()) {
                        val labels = displayTags.map { tagDisplayName(it) }
                        Text(
                            text = labels.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Floating top-left badge. Only one shows at a time: heart wins
        // over star when the venue is both saved and editorially featured,
        // so the user's own state is what they see first.
        when {
            isFavorite -> Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = if (isClosed) BadgeFavoriteColor.copy(alpha = 0.55f) else BadgeFavoriteColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(MARKER_BADGE_SIZE),
            )
            restaurant.featured -> Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (isClosed) BadgeFeaturedColor.copy(alpha = 0.55f) else BadgeFeaturedColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(MARKER_BADGE_SIZE),
            )
        }
    }
}

private val MARKER_CORNER_RADIUS = 10.dp
private val MARKER_TAIL_WIDTH = 10.dp
private val MARKER_TAIL_HEIGHT = 6.dp

// Layout constants exposed for clustering's AABB overlap calculation. The
// pill is `start padding + icon + gap + textColumn + end padding` wide; the
// text column is capped, the rest is fixed, so total width is bounded by
// MARKER_BASE_WIDTH + MARKER_TEXT_COLUMN_MAX_WIDTH.
private val MARKER_TEXT_COLUMN_MAX_WIDTH = 132.dp
private val MARKER_BASE_WIDTH = 6.dp + 32.dp + 8.dp + 10.dp // start + icon + gap + end
internal val MARKER_MAX_WIDTH = MARKER_BASE_WIDTH + MARKER_TEXT_COLUMN_MAX_WIDTH

// The pill body (excluding the tail) is icon (32dp) + top/bottom padding
// (6+6) = 44dp; tail adds another 6dp. Use the body height as the AABB
// height since the tail is a thin triangle that doesn't visually clash
// with neighbouring pills' bodies.
internal val MARKER_BODY_HEIGHT = 44.dp

// Floating-badge size + how far it overhangs each edge of the pill.
// Overhang = SIZE/2 means half of the badge sits inside the pill's
// rounded corner, half outside — the look in the design spec.
internal val MARKER_BADGE_SIZE = 20.dp
internal val MARKER_BADGE_OVERHANG = MARKER_BADGE_SIZE / 2

// Saturated badge colours — pulled out of MaterialTheme on purpose. The
// theme's primary is a wine-leaning red that reads as muted next to the
// brand-red marker pin behind it; the design wants a punchier red for
// the heart. Star uses gold to differentiate at a glance from the heart.
private val BadgeFavoriteColor = Color(0xFFE53935) // Material Red 600
private val BadgeFeaturedColor = Color(0xFFFFC107) // Material Amber 500

/**
 * Conservative pill-width estimate for a given restaurant, used by the
 * clusterer to reason about visual overlap before the bitmap is rendered.
 *
 * Formula: base chrome width + max(name row, tag row), capped at
 * [MARKER_MAX_WIDTH], then doubled by the badge overhang on both sides
 * (always added — most pills don't have a badge but the overhead is small
 * compared to the safety padding the AABB predicate applies anyway).
 * Per-character widths approximate the rendered `labelMedium` (zh name)
 * and `labelSmall` (tag joiner) at typical typography sizes — fine-grained
 * accuracy isn't needed because the clustering predicate already adds a
 * safety padding.
 */
internal fun estimatedMarkerWidth(
    restaurant: com.novawerk.berlinfoodmap.domain.restaurant.Restaurant,
): androidx.compose.ui.unit.Dp {
    val name = restaurant.name.zh
    // Rough char widths in dp at the rendered text styles. CJK fonts are
    // squarer than Latin, so we use the same ~14dp/char for the name row
    // (labelMedium ≈ 14sp) and a slimmer ~9dp average for the tag row.
    val nameWidth = (name.length * 14).dp
    val tags = restaurant.cardTags()
    val tagsText = if (tags.isEmpty()) {
        ""
    } else {
        tags.joinToString(" · ") { tag ->
            // Display labels are short enough that we approximate length
            // by the tag's enum name; doesn't need to be exact.
            tag.name
        }
    }
    val tagsWidth = (tagsText.length * 9).dp
    val textColumnWidth = maxOf(nameWidth, tagsWidth)
        .coerceAtMost(MARKER_TEXT_COLUMN_MAX_WIDTH)
    val pillWidth = (MARKER_BASE_WIDTH + textColumnWidth).coerceAtMost(MARKER_MAX_WIDTH)
    // Bitmap is wrapped in symmetric overhang padding (see top of file)
    // so the AABB rect that the clusterer sees is `pill + 2 * overhang`.
    return pillWidth + MARKER_BADGE_OVERHANG * 2
}

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
