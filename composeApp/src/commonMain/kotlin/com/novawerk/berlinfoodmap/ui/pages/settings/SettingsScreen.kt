package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
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

    // No top app bar — Settings is a tab destination reached via the bottom
    // navigation, not a pushed screen. The bottom-nav label already says
    // 设置 / Settings, so a duplicate header in the body is just wasted
    // vertical space. statusBarsPadding keeps content out from under the
    // notch / camera cutout.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
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

        // Feedback — in-app form, no GitHub account required
        MenuRow(
            title = stringResource(Res.string.feedback_title),
            supportingText = stringResource(Res.string.feedback_subtitle),
            onClick = { showFeedbackSheet = true },
        )

        // About — story, contribute, and contributors live in one merged sheet
        MenuRow(
            title = stringResource(Res.string.about_title),
            supportingText = stringResource(Res.string.about_subtitle),
            onClick = { showAboutSheet = true },
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
            title = stringResource(Res.string.team_pinwo_name),
            supportingText = stringResource(Res.string.team_pinwo_role),
            onClick = { showPinwoSheet = true },
        )

        HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

        // Legal & credits cluster — privacy first (legally required as its
        // own discoverable entry), then the merged licences-and-attributions
        // sheet that also folds in the icon credits that used to live as a
        // separate inline footer.
        MenuRow(
            title = stringResource(Res.string.privacy_policy),
            supportingText = stringResource(Res.string.privacy_policy_subtitle),
            onClick = { urlLauncher.open(PRIVACY_POLICY_URL) },
        )
        MenuRow(
            title = stringResource(Res.string.licenses_title),
            supportingText = stringResource(Res.string.licenses_subtitle),
            onClick = { showLicensesSheet = true },
        )

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

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
                text = "${BuildKonfig.VERSION_NAME} (${BuildKonfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
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
