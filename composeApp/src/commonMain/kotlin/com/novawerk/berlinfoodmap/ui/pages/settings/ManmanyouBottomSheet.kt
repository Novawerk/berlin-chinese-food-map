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
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.team_manmanyou_name
import berlinfoodmap.composeapp.generated.resources.team_manmanyou_role
import berlinfoodmap.composeapp.generated.resources.team_manmanyou_bio
import berlinfoodmap.composeapp.generated.resources.team_manmanyou_visit

private const val MANMANYOU_XHS_URL =
    "https://www.xiaohongshu.com/user/profile/6251a0cc000000001000cc43?xsec_token=AB9CHAQVkTAFzuk1_HXLnf8KcpjAtB7aQI0LSMUKpp-rc%3D&xsec_source=pc_search"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManmanyouBottomSheet(onDismiss: () -> Unit) {
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
                text = stringResource(Res.string.team_manmanyou_name),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.team_manmanyou_role),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Text(
                text = stringResource(Res.string.team_manmanyou_bio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(20.dp))
            FilledTonalButton(
                onClick = { urlLauncher.open(MANMANYOU_XHS_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.team_manmanyou_visit))
            }
        }
    }
}
