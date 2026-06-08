package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TagChipsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun togglingAChipReportsThatTag() {
        var toggled: Tag? = null
        composeRule.setContent {
            MaterialTheme {
                TagChips(selected = emptySet(), onToggle = { toggled = it }, onClear = {})
            }
        }
        // Format chips sit past the 10 regional chips in a horizontal scroller.
        composeRule.onNodeWithTag(tagChipTestTag(Tag.HOTPOT)).performScrollTo().performClick()
        assertEquals(Tag.HOTPOT, toggled)
    }

    @Test
    fun allChipInvokesClear() {
        var cleared = false
        composeRule.setContent {
            MaterialTheme {
                TagChips(selected = setOf(Tag.SICHUAN), onToggle = {}, onClear = { cleared = true })
            }
        }
        composeRule.onNodeWithTag(TAG_CHIP_ALL).performClick()
        assertTrue(cleared)
    }

    @Test
    fun selectionIsReflectedInChipState() {
        composeRule.setContent {
            var selected by remember { mutableStateOf(emptySet<Tag>()) }
            MaterialTheme {
                TagChips(
                    selected = selected,
                    onToggle = { selected = selected + it },
                    onClear = { selected = emptySet() },
                )
            }
        }
        // No selection -> the "All" chip is the selected one.
        composeRule.onNodeWithTag(TAG_CHIP_ALL).assertIsSelected()
        composeRule.onNodeWithTag(tagChipTestTag(Tag.SICHUAN)).assertIsNotSelected()

        composeRule.onNodeWithTag(tagChipTestTag(Tag.SICHUAN)).performScrollTo().performClick()

        composeRule.onNodeWithTag(tagChipTestTag(Tag.SICHUAN)).assertIsSelected()
        composeRule.onNodeWithTag(TAG_CHIP_ALL).assertIsNotSelected()
    }
}
