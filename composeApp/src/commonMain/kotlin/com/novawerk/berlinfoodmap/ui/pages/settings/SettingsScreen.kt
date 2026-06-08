package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.novawerk.berlinfoodmap.ui.theme.novawerkWordmarkFamily
import com.novawerk.berlinfoodmap.BuildKonfig
import com.novawerk.berlinfoodmap.domain.feedback.FeedbackRepository
import com.novawerk.berlinfoodmap.ui.components.MenuRow
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.nova_star
import berlinfoodmap.composeapp.generated.resources.pinwo_p_bug
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
    var showLanguageSheet by remember { mutableStateOf(false) }
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
            SegmentedChoice(
                options = listOf(
                    "system" to Res.string.system_default,
                    "light" to Res.string.light,
                    "dark" to Res.string.dark,
                ),
                selected = currentDarkMode,
                onSelect = onDarkModeChange,
            )
            Spacer(Modifier.height(24.dp))
        }

        // Language pairs visually with Appearance (header + full-width pill),
        // but opens a selection sheet instead of an inline segmented control
        // so it scales as more languages are supported. See LanguageBottomSheet.
        item("language") {
            val languageLabel = when (currentLanguage) {
                "en" -> stringResource(Res.string.english)
                "zh" -> stringResource(Res.string.chinese)
                else -> stringResource(Res.string.system_default)
            }
            Text(
                text = stringResource(Res.string.language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            LanguageSelector(
                label = languageLabel,
                onClick = { showLanguageSheet = true },
            )
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
                titleStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = novawerkWordmarkFamily(),
                    letterSpacing = (-0.02).em,
                ),
                leading = {
                    Icon(
                        painter = painterResource(Res.drawable.nova_star),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                },
                onClick = { showNovawerkSheet = true },
            )
        }
        item("team-pinwo") {
            MenuRow(
                title = stringResource(Res.string.team_pinwo_name),
                supportingText = stringResource(Res.string.team_pinwo_role),
                titleStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                leading = {
                    Icon(
                        painter = painterResource(Res.drawable.pinwo_p_bug),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                },
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
    if (showLanguageSheet) {
        LanguageBottomSheet(
            current = currentLanguage,
            onSelect = onLanguageChange,
            onDismiss = { showLanguageSheet = false },
        )
    }
}

/**
 * Warm-neutral segmented control for Appearance / Language. A pill-shaped
 * track ([surfaceContainerHigh]) with the selected option lifted onto a
 * [surfaceBright] pill in ink — quiet chrome that fits the cream/ink palette,
 * instead of the off-brand olive of M3's default SegmentedButton.
 */
@Composable
private fun <T> SegmentedChoice(
    options: List<Pair<T, StringResource>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((value, labelRes) in options) {
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(2.dp, pill, clip = false)
                                .background(MaterialTheme.colorScheme.surfaceBright, pill)
                        } else {
                            Modifier
                        },
                    )
                    .clip(pill)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/**
 * Full-width language selector that visually rhymes with [SegmentedChoice]
 * (same [surfaceContainerHigh] pill) so Language reads as a primary control
 * paired with Appearance — but it shows only the current value + a chevron
 * and opens [LanguageBottomSheet] on tap, which scales to any number of
 * languages where a segmented row would not.
 */
@Composable
private fun LanguageSelector(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
