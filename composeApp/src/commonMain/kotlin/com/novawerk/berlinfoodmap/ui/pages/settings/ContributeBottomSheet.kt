package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.contribute_title
import berlinfoodmap.composeapp.generated.resources.contribute_intro
import berlinfoodmap.composeapp.generated.resources.contribute_what_title
import berlinfoodmap.composeapp.generated.resources.contribute_what_1
import berlinfoodmap.composeapp.generated.resources.contribute_what_2
import berlinfoodmap.composeapp.generated.resources.contribute_what_3
import berlinfoodmap.composeapp.generated.resources.contribute_what_4
import berlinfoodmap.composeapp.generated.resources.contribute_how_title
import berlinfoodmap.composeapp.generated.resources.contribute_step_1
import berlinfoodmap.composeapp.generated.resources.contribute_step_2
import berlinfoodmap.composeapp.generated.resources.contribute_step_3
import berlinfoodmap.composeapp.generated.resources.contribute_after_title
import berlinfoodmap.composeapp.generated.resources.contribute_after
import berlinfoodmap.composeapp.generated.resources.contribute_action

private const val GITHUB_REPO_URL = "https://github.com/Novawerk/berlin-chinese-food-map"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributeBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val urlLauncher = rememberUrlLauncher()

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
                text = stringResource(Res.string.contribute_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            BodyParagraph(Res.string.contribute_intro)

            Spacer(Modifier.height(16.dp))
            SubsectionTitle(Res.string.contribute_what_title)
            BulletItem(Res.string.contribute_what_1)
            BulletItem(Res.string.contribute_what_2)
            BulletItem(Res.string.contribute_what_3)
            BulletItem(Res.string.contribute_what_4)

            Spacer(Modifier.height(16.dp))
            SubsectionTitle(Res.string.contribute_how_title)
            BulletItem(Res.string.contribute_step_1)
            BulletItem(Res.string.contribute_step_2)
            BulletItem(Res.string.contribute_step_3)

            Spacer(Modifier.height(16.dp))
            SubsectionTitle(Res.string.contribute_after_title)
            BodyParagraph(Res.string.contribute_after)

            Spacer(Modifier.height(20.dp))
            FilledTonalButton(
                onClick = { urlLauncher.open(GITHUB_REPO_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.contribute_action))
            }
        }
    }
}

@Composable
private fun SubsectionTitle(res: StringResource) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun BodyParagraph(res: StringResource) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun BulletItem(res: StringResource) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
