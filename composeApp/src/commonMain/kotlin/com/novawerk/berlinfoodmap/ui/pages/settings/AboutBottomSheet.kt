package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.ui.components.AppBottomSheet
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.story_title
import berlinfoodmap.composeapp.generated.resources.story_body
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
import berlinfoodmap.composeapp.generated.resources.contributors_title
import berlinfoodmap.composeapp.generated.resources.contributors_intro
import berlinfoodmap.composeapp.generated.resources.contributors_count
import berlinfoodmap.composeapp.generated.resources.contributors_empty

private const val GITHUB_REPO_URL = "https://github.com/Novawerk/berlin-chinese-food-map"

@Composable
fun AboutBottomSheet(onDismiss: () -> Unit) {
    val urlLauncher = rememberUrlLauncher()
    val contributors = DATA_CONTRIBUTORS

    AppBottomSheet(onDismiss = onDismiss) {
        // Items are split by section; the contributors block uses
        // `itemsIndexed` so each row is its own lazy entry. The rest of
        // the long-form prose stays grouped because there's no benefit
        // to laziness for a fixed body of text.
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.story_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Text(
                    text = stringResource(Res.string.story_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionDivider()
                Text(
                    text = stringResource(Res.string.contribute_title),
                    style = MaterialTheme.typography.titleLarge,
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

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionDivider()
                Text(
                    text = stringResource(Res.string.contributors_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                BodyParagraph(Res.string.contributors_intro)
                Spacer(Modifier.height(12.dp))

                if (contributors.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.contributors_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(
                            Res.string.contributors_count,
                            contributors.size,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }

        itemsIndexed(contributors) { index, contributor ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (index > 0) HorizontalDivider()
                ContributorRow(
                    contributor = contributor,
                    onClick = contributor.github?.let {
                        { urlLauncher.open("https://github.com/$it") }
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(Modifier.height(24.dp))
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
private fun ContributorRow(
    contributor: Contributor,
    onClick: (() -> Unit)?,
) {
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = contributor.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (contributor.github != null) {
                Text(
                    text = "@${contributor.github}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
