package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.ui.components.AppBottomSheet
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.language
import berlinfoodmap.composeapp.generated.resources.system_default
import berlinfoodmap.composeapp.generated.resources.english
import berlinfoodmap.composeapp.generated.resources.chinese

/**
 * Language picker as a selection sheet rather than an inline segmented
 * control — a horizontal pill row only fits a handful of options, so as the
 * supported-language list grows this scales to any number. Each option is a
 * tappable row with a check on the current selection; picking one applies it
 * and dismisses. To add a language, append it to [LANGUAGE_OPTIONS].
 */
private val LANGUAGE_OPTIONS = listOf(
    null to Res.string.system_default,
    "en" to Res.string.english,
    "zh" to Res.string.chinese,
)

@Composable
fun LanguageBottomSheet(
    current: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        item {
            Text(
                text = stringResource(Res.string.language),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(LANGUAGE_OPTIONS) { (value, labelRes) ->
            val selected = value == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelect(value)
                        onDismiss()
                    }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
