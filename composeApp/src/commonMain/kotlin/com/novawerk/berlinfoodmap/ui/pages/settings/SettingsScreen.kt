package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository
import kotlinx.coroutines.launch
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
import berlinfoodmap.composeapp.generated.resources.clear_favorites
import berlinfoodmap.composeapp.generated.resources.privacy_policy
import berlinfoodmap.composeapp.generated.resources.icon_credits
import berlinfoodmap.composeapp.generated.resources.icon_credits_value

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentDarkMode: String,
    currentLanguage: String?,
    onDarkModeChange: (String) -> Unit,
    onLanguageChange: (String?) -> Unit,
    favoritesRepository: FavoritesRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
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

            // About section
            Text(
                text = stringResource(Res.string.about),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

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

            // Clear favorites
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.clear_favorites),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = {
                        scope.launch { favoritesRepository.clearAll() }
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.clear_favorites),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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
        }
    }
}
