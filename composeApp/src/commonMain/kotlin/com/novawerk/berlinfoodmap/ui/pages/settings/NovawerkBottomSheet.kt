package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.ui.components.AppBottomSheet
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.nova_star
import berlinfoodmap.composeapp.generated.resources.team_novawerk_name
import berlinfoodmap.composeapp.generated.resources.team_novawerk_role
import berlinfoodmap.composeapp.generated.resources.team_novawerk_bio
import berlinfoodmap.composeapp.generated.resources.team_novawerk_visit

private const val NOVAWERK_URL = "https://novawerk.io"

@Composable
fun NovawerkBottomSheet(onDismiss: () -> Unit) {
    val urlLauncher = rememberUrlLauncher()

    AppBottomSheet(onDismiss = onDismiss) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(Res.drawable.nova_star),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.team_novawerk_name),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.team_novawerk_role),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                Text(
                    text = stringResource(Res.string.team_novawerk_bio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(20.dp))
                FilledTonalButton(
                    onClick = { urlLauncher.open(NOVAWERK_URL) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.team_novawerk_visit))
                }
            }
        }
    }
}
