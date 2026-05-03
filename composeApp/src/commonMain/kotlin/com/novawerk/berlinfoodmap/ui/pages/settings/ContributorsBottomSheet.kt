package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.contributors_title
import berlinfoodmap.composeapp.generated.resources.contributors_intro
import berlinfoodmap.composeapp.generated.resources.contributors_count
import berlinfoodmap.composeapp.generated.resources.contributors_empty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributorsBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val urlLauncher = rememberUrlLauncher()
    val contributors = DATA_CONTRIBUTORS

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(Res.string.contributors_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (contributors.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.contributors_count, contributors.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Text(
                text = stringResource(Res.string.contributors_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
            )

            if (contributors.isEmpty()) {
                Text(
                    text = stringResource(Res.string.contributors_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                contributors.forEachIndexed { index, contributor ->
                    if (index > 0) HorizontalDivider()
                    ContributorRow(
                        contributor = contributor,
                        onClick = contributor.github?.let {
                            { urlLauncher.open("https://github.com/$it") }
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ContributorRow(
    contributor: Contributor,
    onClick: (() -> Unit)?,
) {
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = contributor.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (contributor.github != null) {
                Text(
                    text = "@${contributor.github}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
