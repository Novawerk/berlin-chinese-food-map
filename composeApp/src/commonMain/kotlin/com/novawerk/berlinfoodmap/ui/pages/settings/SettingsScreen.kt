package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.ui.components.MenuRow
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.settings
import berlinfoodmap.composeapp.generated.resources.back
import berlinfoodmap.composeapp.generated.resources.dark_mode
import berlinfoodmap.composeapp.generated.resources.language
import berlinfoodmap.composeapp.generated.resources.system_default
import berlinfoodmap.composeapp.generated.resources.light
import berlinfoodmap.composeapp.generated.resources.dark
import berlinfoodmap.composeapp.generated.resources.english
import berlinfoodmap.composeapp.generated.resources.chinese
import berlinfoodmap.composeapp.generated.resources.about
import berlinfoodmap.composeapp.generated.resources.version
import berlinfoodmap.composeapp.generated.resources.privacy_policy
import berlinfoodmap.composeapp.generated.resources.icon_credits
import berlinfoodmap.composeapp.generated.resources.icon_credits_value
import berlinfoodmap.composeapp.generated.resources.contribute_title
import berlinfoodmap.composeapp.generated.resources.team_title
import berlinfoodmap.composeapp.generated.resources.team_novawerk_name
import berlinfoodmap.composeapp.generated.resources.team_novawerk_role
import berlinfoodmap.composeapp.generated.resources.team_manmanyou_name
import berlinfoodmap.composeapp.generated.resources.team_manmanyou_role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentDarkMode: String,
    currentLanguage: String?,
    onDarkModeChange: (String) -> Unit,
    onLanguageChange: (String?) -> Unit,
    onBack: () -> Unit,
) {
    var showContributeSheet by remember { mutableStateOf(false) }
    var showNovawerkSheet by remember { mutableStateOf(false) }
    var showManmanyouSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Dark mode section
            Text(
                text = stringResource(Res.string.dark_mode),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf("system" to Res.string.system_default, "light" to Res.string.light, "dark" to Res.string.dark)
                options.forEachIndexed { index, (value, labelRes) ->
                    SegmentedButton(
                        selected = currentDarkMode == value,
                        onClick = { onDarkModeChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Language section
            Text(
                text = stringResource(Res.string.language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(null to Res.string.system_default, "en" to Res.string.english, "zh" to Res.string.chinese)
                options.forEachIndexed { index, (value, labelRes) ->
                    SegmentedButton(
                        selected = currentLanguage == value,
                        onClick = { onLanguageChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()

            // About section
            Text(
                text = stringResource(Res.string.about),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            MenuRow(
                title = stringResource(Res.string.contribute_title),
                onClick = { showContributeSheet = true },
            )

            // Team section
            Text(
                text = stringResource(Res.string.team_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            MenuRow(
                title = stringResource(Res.string.team_novawerk_name),
                supportingText = stringResource(Res.string.team_novawerk_role),
                onClick = { showNovawerkSheet = true },
            )
            MenuRow(
                title = stringResource(Res.string.team_manmanyou_name),
                supportingText = stringResource(Res.string.team_manmanyou_role),
                onClick = { showManmanyouSheet = true },
            )

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

            // Version row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.version),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Privacy policy
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.privacy_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Icon credits (Flaticon attribution)
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.icon_credits),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(Res.string.icon_credits_value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showContributeSheet) {
        ContributeBottomSheet(onDismiss = { showContributeSheet = false })
    }
    if (showNovawerkSheet) {
        NovawerkBottomSheet(onDismiss = { showNovawerkSheet = false })
    }
    if (showManmanyouSheet) {
        ManmanyouBottomSheet(onDismiss = { showManmanyouSheet = false })
    }
}
