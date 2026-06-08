package com.novawerk.berlinfoodmap.ui.pages.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.BuildKonfig
import com.novawerk.berlinfoodmap.domain.feedback.FeedbackCategory
import com.novawerk.berlinfoodmap.domain.feedback.FeedbackRepository
import com.novawerk.berlinfoodmap.getPlatform
import com.novawerk.berlinfoodmap.ui.components.AppBottomSheet
import com.novawerk.berlinfoodmap.ui.locale.LocalAppLocale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.feedback_category_bug
import berlinfoodmap.composeapp.generated.resources.feedback_category_idea
import berlinfoodmap.composeapp.generated.resources.feedback_category_label
import berlinfoodmap.composeapp.generated.resources.feedback_category_other
import berlinfoodmap.composeapp.generated.resources.feedback_category_restaurant
import berlinfoodmap.composeapp.generated.resources.feedback_close
import berlinfoodmap.composeapp.generated.resources.feedback_contact_hint
import berlinfoodmap.composeapp.generated.resources.feedback_contact_label
import berlinfoodmap.composeapp.generated.resources.feedback_error
import berlinfoodmap.composeapp.generated.resources.feedback_intro
import berlinfoodmap.composeapp.generated.resources.feedback_message_hint
import berlinfoodmap.composeapp.generated.resources.feedback_message_label
import berlinfoodmap.composeapp.generated.resources.feedback_submit
import berlinfoodmap.composeapp.generated.resources.feedback_submitting
import berlinfoodmap.composeapp.generated.resources.feedback_success_body
import berlinfoodmap.composeapp.generated.resources.feedback_success_title
import berlinfoodmap.composeapp.generated.resources.feedback_title

private const val MESSAGE_MAX_LENGTH = 4000
private const val CONTACT_MAX_LENGTH = 200

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackBottomSheet(
    repository: FeedbackRepository,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val locale = LocalAppLocale.current

    var category by remember { mutableStateOf(FeedbackCategory.BUG) }
    var message by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submittedId by remember { mutableStateOf<String?>(null) }

    val errorCopy = stringResource(Res.string.feedback_error)

    AppBottomSheet(onDismiss = onDismiss) {
        if (submittedId != null) {
            item { FeedbackSuccessContent(onClose = onDismiss) }
        } else {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.feedback_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.feedback_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(Res.string.feedback_category_label),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CategoryChip(FeedbackCategory.BUG, category) { category = it }
                        CategoryChip(FeedbackCategory.IDEA, category) { category = it }
                        CategoryChip(FeedbackCategory.RESTAURANT, category) { category = it }
                        CategoryChip(FeedbackCategory.OTHER, category) { category = it }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = {
                            if (it.length <= MESSAGE_MAX_LENGTH) message = it
                        },
                        label = { Text(stringResource(Res.string.feedback_message_label)) },
                        placeholder = { Text(stringResource(Res.string.feedback_message_hint)) },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        minLines = 5,
                        maxLines = 10,
                        isError = error != null,
                        enabled = !submitting,
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = contact,
                        onValueChange = {
                            if (it.length <= CONTACT_MAX_LENGTH) contact = it
                        },
                        label = { Text(stringResource(Res.string.feedback_contact_label)) },
                        placeholder = { Text(stringResource(Res.string.feedback_contact_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !submitting,
                    )

                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (message.trim().isEmpty() || submitting) return@Button
                            submitting = true
                            error = null
                            scope.launch {
                                val result = repository.submit(
                                    category = category,
                                    message = message,
                                    contact = contact.takeIf { it.isNotBlank() },
                                    appVersion = "${BuildKonfig.VERSION_NAME} (${BuildKonfig.VERSION_CODE})",
                                    platform = getPlatform().name,
                                    locale = locale,
                                )
                                submitting = false
                                result.fold(
                                    onSuccess = { id -> submittedId = id },
                                    onFailure = { error = errorCopy },
                                )
                            }
                        },
                        enabled = !submitting && message.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.feedback_submitting))
                        } else {
                            Text(stringResource(Res.string.feedback_submit))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    target: FeedbackCategory,
    selected: FeedbackCategory,
    onSelect: (FeedbackCategory) -> Unit,
) {
    val labelRes = when (target) {
        FeedbackCategory.BUG -> Res.string.feedback_category_bug
        FeedbackCategory.IDEA -> Res.string.feedback_category_idea
        FeedbackCategory.RESTAURANT -> Res.string.feedback_category_restaurant
        FeedbackCategory.OTHER -> Res.string.feedback_category_other
    }
    FilterChip(
        selected = selected == target,
        onClick = { onSelect(target) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun FeedbackSuccessContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(Res.string.feedback_success_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.feedback_success_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.feedback_close))
        }
    }
}
