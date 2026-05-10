package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard scrollable bottom sheet for the app.
 *
 *  - Always [skipPartiallyExpanded] — none of our sheets need a half-state.
 *  - Wraps content in a [LazyColumn] (not `Column.verticalScroll`): the
 *    sheet's built-in nested-scroll connection plays nicely with
 *    LazyColumn at the bottom edge, but races with `verticalScroll`'s
 *    overscroll bounce — the bounce is interpreted as a drag-to-dismiss
 *    gesture and the sheet visibly jitters.
 *  - Applies the standard horizontal/bottom padding the design uses for
 *    legal / team / about / feedback content. Override with
 *    [contentPadding] if you need something different.
 *
 * Usage:
 * ```
 * AppBottomSheet(onDismiss = onDismiss) {
 *     item { Header() }
 *     items(rows) { Row(it) }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = DefaultBottomSheetContentPadding,
    content: LazyListScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            content = content,
        )
    }
}

private val DefaultBottomSheetContentPadding = PaddingValues(
    start = 24.dp,
    end = 24.dp,
    bottom = 32.dp,
)
