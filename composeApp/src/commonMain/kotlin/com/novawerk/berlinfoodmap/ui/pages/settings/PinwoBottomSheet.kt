package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.ui.components.AppBottomSheet
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.team_pinwo_name
import berlinfoodmap.composeapp.generated.resources.team_pinwo_role
import berlinfoodmap.composeapp.generated.resources.team_pinwo_bio
import berlinfoodmap.composeapp.generated.resources.team_pinwo_visit
import berlinfoodmap.composeapp.generated.resources.team_pinwo_community

private const val PINWO_SITE_URL = "https://pinwo.de"
private const val PINWO_COMMUNITY_URL =
    "https://chat.whatsapp.com/BShah69krZo3HUnjPkPZvg?mode=gi_t"

@Composable
fun PinwoBottomSheet(onDismiss: () -> Unit) {
    val urlLauncher = rememberUrlLauncher()

    AppBottomSheet(onDismiss = onDismiss) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.team_pinwo_name),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.team_pinwo_role),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                Text(
                    text = stringResource(Res.string.team_pinwo_bio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { urlLauncher.open(PINWO_SITE_URL) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.team_pinwo_visit))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { urlLauncher.open(PINWO_COMMUNITY_URL) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.team_pinwo_community))
                }
            }
        }
    }
}
