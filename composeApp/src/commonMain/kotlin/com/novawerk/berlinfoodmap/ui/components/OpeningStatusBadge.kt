package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.status_closed_short
import berlinfoodmap.composeapp.generated.resources.status_closing_soon
import berlinfoodmap.composeapp.generated.resources.status_opening_soon
import com.novawerk.berlinfoodmap.domain.restaurant.OpeningStatus
import com.novawerk.berlinfoodmap.domain.restaurant.computeOpeningStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val OPENING_SOON_MIN = 60
private const val CLOSING_SOON_MIN = 30

/** testTag on [OpeningStatusBadge]'s pill — absent from the tree when the venue is open/24h/unknown. */
const val OPENING_STATUS_BADGE_TAG = "openingStatusBadge"

private enum class StatusFlavor { Closed, OpeningSoon, ClosingSoon }

private data class StatusDescriptor(
    val flavor: StatusFlavor,
    val labelRes: StringResource,
)

private fun describe(status: OpeningStatus): StatusDescriptor? = when (status) {
    is OpeningStatus.Closed -> if (status.minutesUntilOpen <= OPENING_SOON_MIN) {
        StatusDescriptor(StatusFlavor.OpeningSoon, Res.string.status_opening_soon)
    } else {
        StatusDescriptor(StatusFlavor.Closed, Res.string.status_closed_short)
    }
    is OpeningStatus.Open -> if (status.minutesUntilClose <= CLOSING_SOON_MIN) {
        StatusDescriptor(StatusFlavor.ClosingSoon, Res.string.status_closing_soon)
    } else null
    OpeningStatus.AlwaysOpen, OpeningStatus.Unknown -> null
}

@Composable
private fun StatusFlavor.color(): Color = when (this) {
    StatusFlavor.Closed -> MaterialTheme.colorScheme.error
    StatusFlavor.OpeningSoon, StatusFlavor.ClosingSoon -> MaterialTheme.colorScheme.tertiary
}

/**
 * True when the venue is currently shut (Closed or OpeningSoon — both flavours
 * are "not open right now" from the user's POV). Map cards use this to dim
 * the whole row so "closed" is obvious before the user even spots the dot.
 *
 * Returns false for Open / AlwaysOpen / Unknown so we don't gray out venues
 * that are open or where we just lack data.
 */
@Composable
fun rememberIsCurrentlyClosed(periods: List<String>): Boolean {
    val status = remember(periods) { computeOpeningStatus(periods) }
    return status is OpeningStatus.Closed
}

/**
 * Tiny coloured dot with a surface-coloured ring so it reads against any
 * thumbnail. Renders nothing when the venue is open / 24h / unknown — the
 * absence of a dot itself signals "currently open".
 *
 * Designed for the corner of a map card thumbnail (NearbyCard 44dp,
 * MiniRestaurantCard 32dp) where the previous text pill was too heavy.
 *
 * Color codes:
 *  - red (`error`)      → closed, won't open within an hour
 *  - amber (`tertiary`) → opens within an hour OR closes within 30 min
 */
@Composable
fun OpeningStatusDot(
    periods: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    ringWidth: Dp = 2.dp,
) {
    val status = remember(periods) { computeOpeningStatus(periods) }
    val descriptor = describe(status) ?: return
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surface, CircleShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(ringWidth)
                .background(descriptor.flavor.color(), CircleShape),
        )
    }
}

/**
 * Compact text pill — kept for places where there's room to spell out the
 * status (e.g. detail-screen surfaces). Map cards should use [OpeningStatusDot]
 * instead since the pill text crowds the title row.
 */
@Composable
fun OpeningStatusBadge(
    periods: List<String>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val status = remember(periods) { computeOpeningStatus(periods) }
    val descriptor = describe(status) ?: return
    val (container, content) = when (descriptor.flavor) {
        StatusFlavor.Closed -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        StatusFlavor.OpeningSoon, StatusFlavor.ClosingSoon ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    val hPad = if (compact) 6.dp else 8.dp
    val vPad = if (compact) 2.dp else 3.dp
    Surface(
        color = container,
        shape = RoundedCornerShape(50),
        modifier = modifier.testTag(OPENING_STATUS_BADGE_TAG),
    ) {
        Box(modifier = Modifier.padding(horizontal = hPad, vertical = vPad)) {
            Text(
                text = stringResource(descriptor.labelRes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
        }
    }
}

