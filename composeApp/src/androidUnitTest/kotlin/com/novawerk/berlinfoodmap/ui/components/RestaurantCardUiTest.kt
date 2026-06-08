package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.novawerk.berlinfoodmap.testutil.localizable
import com.novawerk.berlinfoodmap.testutil.restaurant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compose UI tests run headless on the JVM via Robolectric — no emulator. The
 * map screen embeds a native Google Maps view that can't render here, so UI
 * coverage targets the non-map list/detail components instead.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RestaurantCardUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersBilingualName() {
        composeRule.setContent {
            MaterialTheme {
                RestaurantCard(
                    restaurant = restaurant(name = localizable(en = "Golden Dragon", zh = "金龙饭店")),
                    onClick = {},
                )
            }
        }
        composeRule.onNodeWithText("金龙饭店").assertIsDisplayed()
        composeRule.onNodeWithText("Golden Dragon").assertIsDisplayed()
    }

    @Test
    fun clickInvokesCallback() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                RestaurantCard(
                    restaurant = restaurant(),
                    onClick = { clicked = true },
                    modifier = Modifier.testTag("card"),
                )
            }
        }
        composeRule.onNodeWithTag("card").performClick()
        assertTrue(clicked)
    }

    @Test
    fun clickingOneCardInAListReportsThatRestaurant() {
        var selected: String? = null
        val alpha = restaurant(id = "a", name = localizable(en = "Alpha", zh = "甲"))
        val beta = restaurant(id = "b", name = localizable(en = "Beta", zh = "乙"))
        composeRule.setContent {
            MaterialTheme {
                Column {
                    listOf(alpha, beta).forEach { r ->
                        RestaurantCard(
                            restaurant = r,
                            onClick = { selected = r.id },
                            modifier = Modifier.testTag("card_${r.id}"),
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithTag("card_b").performClick()
        assertEquals("b", selected)
    }
}
