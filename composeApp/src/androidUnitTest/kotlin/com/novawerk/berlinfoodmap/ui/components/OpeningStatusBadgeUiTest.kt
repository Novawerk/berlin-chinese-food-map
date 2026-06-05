package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The badge's design contract: it renders nothing when the venue is open / 24h /
 * unknown — the *absence* of a badge is what signals "open". Those two cases are
 * time-independent and so deterministic here; the closed/opening-soon/closing-soon
 * label and threshold logic is covered time-deterministically in OpeningStatusTest.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class OpeningStatusBadgeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hiddenForAlwaysOpenVenue() {
        composeRule.setContent {
            MaterialTheme { OpeningStatusBadge(periods = listOf("OPEN_24H")) }
        }
        composeRule.onNodeWithTag(OPENING_STATUS_BADGE_TAG).assertDoesNotExist()
    }

    @Test
    fun hiddenWhenHoursUnknown() {
        composeRule.setContent {
            MaterialTheme { OpeningStatusBadge(periods = emptyList()) }
        }
        composeRule.onNodeWithTag(OPENING_STATUS_BADGE_TAG).assertDoesNotExist()
    }
}
