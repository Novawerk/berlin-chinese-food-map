package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.BuildKonfig
import com.novawerk.berlinfoodmap.domain.feedback.FeedbackRepository
import com.novawerk.berlinfoodmap.ui.components.MenuRow
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.nova_star
import berlinfoodmap.composeapp.generated.resources.dark_mode
import berlinfoodmap.composeapp.generated.resources.feedback_subtitle
import berlinfoodmap.composeapp.generated.resources.feedback_title
import berlinfoodmap.composeapp.generated.resources.language
import berlinfoodmap.composeapp.generated.resources.system_default
import berlinfoodmap.composeapp.generated.resources.light
import berlinfoodmap.composeapp.generated.resources.dark
import berlinfoodmap.composeapp.generated.resources.english
import berlinfoodmap.composeapp.generated.resources.chinese
import berlinfoodmap.composeapp.generated.resources.version
import berlinfoodmap.composeapp.generated.resources.privacy_policy
import berlinfoodmap.composeapp.generated.resources.privacy_policy_subtitle
import berlinfoodmap.composeapp.generated.resources.about_title
import berlinfoodmap.composeapp.generated.resources.about_subtitle
import berlinfoodmap.composeapp.generated.resources.team_title
import berlinfoodmap.composeapp.generated.resources.team_novawerk_name
import berlinfoodmap.composeapp.generated.resources.team_novawerk_role
import berlinfoodmap.composeapp.generated.resources.team_pinwo_name
import berlinfoodmap.composeapp.generated.resources.team_pinwo_role
import berlinfoodmap.composeapp.generated.resources.licenses_title
import berlinfoodmap.composeapp.generated.resources.licenses_subtitle

private const val PRIVACY_POLICY_URL = "https://berlinfoodmap.novawerk.io/privacy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentDarkMode: String,
    currentLanguage: String?,
    feedbackRepository: FeedbackRepository,
    onDarkModeChange: (String) -> Unit,
    onLanguageChange: (String?) -> Unit,
) {
    var showAboutSheet by remember { mutableStateOf(false) }
    var showNovawerkSheet by remember { mutableStateOf(false) }
    var showPinwoSheet by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showLicensesSheet by remember { mutableStateOf(false) }
    val urlLauncher = rememberUrlLauncher()

    // Settings now lives inside a ModalBottomSheet (mounted by the caller),
    // so the sheet handles top inset and dismissal. We use LazyColumn over
    // `Column.verticalScroll` because the sheet's nested-scroll plays nicely
    // with LazyColumn at the bottom edge — verticalScroll's overscroll
    // bounce gets interpreted as a drag-to-dismiss gesture and jitters.
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item("dark-mode") {
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
        }

        item("language") {
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
        }

        item("feedback") {
            MenuRow(
                title = stringResource(Res.string.feedback_title),
                supportingText = stringResource(Res.string.feedback_subtitle),
                onClick = { showFeedbackSheet = true },
            )
        }

        item("about") {
            MenuRow(
                title = stringResource(Res.string.about_title),
                supportingText = stringResource(Res.string.about_subtitle),
                onClick = { showAboutSheet = true },
            )
        }

        item("team-header") {
            Text(
                text = stringResource(Res.string.team_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
        }
        item("team-novawerk") {
            MenuRow(
                title = stringResource(Res.string.team_novawerk_name),
                supportingText = stringResource(Res.string.team_novawerk_role),
                leading = {
                    Image(
                        painter = painterResource(Res.drawable.nova_star),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                },
                onClick = { showNovawerkSheet = true },
            )
        }
        item("team-pinwo") {
            MenuRow(
                title = stringResource(Res.string.team_pinwo_name),
                supportingText = stringResource(Res.string.team_pinwo_role),
                onClick = { showPinwoSheet = true },
            )
        }

        item("legal-divider") {
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        }
        item("privacy") {
            MenuRow(
                title = stringResource(Res.string.privacy_policy),
                supportingText = stringResource(Res.string.privacy_policy_subtitle),
                onClick = { urlLauncher.open(PRIVACY_POLICY_URL) },
            )
        }
        item("licenses") {
            MenuRow(
                title = stringResource(Res.string.licenses_title),
                supportingText = stringResource(Res.string.licenses_subtitle),
                onClick = { showLicensesSheet = true },
            )
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        }

        item("version") {
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
                    text = "${BuildKonfig.VERSION_NAME} (${BuildKonfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showAboutSheet) {
        AboutBottomSheet(onDismiss = { showAboutSheet = false })
    }
    if (showNovawerkSheet) {
        NovawerkBottomSheet(onDismiss = { showNovawerkSheet = false })
    }
    if (showPinwoSheet) {
        PinwoBottomSheet(onDismiss = { showPinwoSheet = false })
    }
    if (showFeedbackSheet) {
        FeedbackBottomSheet(
            repository = feedbackRepository,
            onDismiss = { showFeedbackSheet = false },
        )
    }
    if (showLicensesSheet) {
        LicensesBottomSheet(onDismiss = { showLicensesSheet = false })
    }
}
