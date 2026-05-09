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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.licenses_title
import berlinfoodmap.composeapp.generated.resources.licenses_intro
import berlinfoodmap.composeapp.generated.resources.licenses_copyright
import berlinfoodmap.composeapp.generated.resources.licenses_code_title
import berlinfoodmap.composeapp.generated.resources.licenses_code_body
import berlinfoodmap.composeapp.generated.resources.licenses_code_view
import berlinfoodmap.composeapp.generated.resources.licenses_thirdparty_title
import berlinfoodmap.composeapp.generated.resources.licenses_thirdparty_body
import berlinfoodmap.composeapp.generated.resources.licenses_thirdparty_view

private const val CODE_LICENSE_URL =
    "https://github.com/Novawerk/berlin-chinese-food-map/blob/main/LICENSE"
private const val REPO_URL =
    "https://github.com/Novawerk/berlin-chinese-food-map"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesBottomSheet(onDismiss: () -> Unit) {
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
                text = stringResource(Res.string.licenses_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(Res.string.licenses_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.licenses_copyright),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            LicenseSection(
                titleRes = Res.string.licenses_code_title,
                bodyRes = Res.string.licenses_code_body,
                actionRes = Res.string.licenses_code_view,
                onAction = { urlLauncher.open(CODE_LICENSE_URL) },
            )

            LicenseSection(
                titleRes = Res.string.licenses_thirdparty_title,
                bodyRes = Res.string.licenses_thirdparty_body,
                actionRes = Res.string.licenses_thirdparty_view,
                onAction = { urlLauncher.open(REPO_URL) },
            )
        }
    }
}

@Composable
private fun LicenseSection(
    titleRes: org.jetbrains.compose.resources.StringResource,
    bodyRes: org.jetbrains.compose.resources.StringResource,
    actionRes: org.jetbrains.compose.resources.StringResource,
    onAction: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    FilledTonalButton(
        onClick = onAction,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(actionRes))
    }
}
