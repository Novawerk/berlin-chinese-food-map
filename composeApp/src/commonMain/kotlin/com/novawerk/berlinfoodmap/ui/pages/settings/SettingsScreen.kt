package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher
import org.jetbrains.compose.resources.StringResource
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
import berlinfoodmap.composeapp.generated.resources.team_title
import berlinfoodmap.composeapp.generated.resources.team_description
import berlinfoodmap.composeapp.generated.resources.team_novawerk_name
import berlinfoodmap.composeapp.generated.resources.team_novawerk_role
import berlinfoodmap.composeapp.generated.resources.team_novawerk_bio
import berlinfoodmap.composeapp.generated.resources.team_manmanyou_name
import berlinfoodmap.composeapp.generated.resources.team_manmanyou_role
import berlinfoodmap.composeapp.generated.resources.team_manmanyou_bio
import berlinfoodmap.composeapp.generated.resources.team_thanks_title
import berlinfoodmap.composeapp.generated.resources.team_thanks

private const val GITHUB_REPO_URL = "https://github.com/Novawerk/berlin-chinese-food-map"
private const val NOVAWERK_URL = "https://novawerk.io"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentDarkMode: String,
    currentLanguage: String?,
    onDarkModeChange: (String) -> Unit,
    onLanguageChange: (String?) -> Unit,
    onBack: () -> Unit,
) {
    val urlLauncher = rememberUrlLauncher()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        }
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
            Spacer(Modifier.height(16.dp))

            // Contribute section
            SectionTitle(Res.string.contribute_title)
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

            Spacer(Modifier.height(16.dp))
            FilledTonalButton(
                onClick = { urlLauncher.open(GITHUB_REPO_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.contribute_action))
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Team section
            SectionTitle(Res.string.team_title)
            BodyParagraph(Res.string.team_description)

            Spacer(Modifier.height(12.dp))
            TeamMemberCard(
                name = stringResource(Res.string.team_novawerk_name),
                role = stringResource(Res.string.team_novawerk_role),
                bio = stringResource(Res.string.team_novawerk_bio),
                onClick = { urlLauncher.open(NOVAWERK_URL) },
            )
            Spacer(Modifier.height(8.dp))
            TeamMemberCard(
                name = stringResource(Res.string.team_manmanyou_name),
                role = stringResource(Res.string.team_manmanyou_role),
                bio = stringResource(Res.string.team_manmanyou_bio),
                onClick = null,
            )

            Spacer(Modifier.height(20.dp))
            SubsectionTitle(Res.string.team_thanks_title)
            BodyParagraph(Res.string.team_thanks)

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // About section
            Text(
                text = stringResource(Res.string.about),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

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

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.privacy_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

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
}

@Composable
private fun SectionTitle(res: StringResource) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 12.dp),
    )
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

@Composable
private fun TeamMemberCard(
    name: String,
    role: String,
    bio: String,
    onClick: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(16.dp)
    val surfaceModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Surface(
        modifier = surfaceModifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = shape,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = role,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
