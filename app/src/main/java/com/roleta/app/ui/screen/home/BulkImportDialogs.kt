package com.roleta.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.roleta.app.data.repository.BulkImportParser
import com.roleta.app.data.repository.BulkImportResult
import com.roleta.app.data.repository.ParsedBulkImport
import com.roleta.app.data.repository.SkipReason
import com.roleta.app.ui.component.pluralize

@Composable
fun BulkImportDialog(
    existingListNames: List<String>,
    isImporting: Boolean,
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) { BulkImportParser.parse(text) }
    val matchingList = parsed?.let { p -> existingListNames.firstOrNull { it.equals(p.title, ignoreCase = true) } }
    val clipboard = LocalClipboardManager.current
    val canImport = parsed != null && parsed.items.isNotEmpty() && !isImporting

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from text") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "The first line becomes the list name. Each line after it is an item.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Weekend Trips\nBaguio\nLa Union\nBatangas") },
                    minLines = 6,
                    maxLines = 10,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = {
                        val clip = clipboard.getText()?.text.orEmpty()
                        if (clip.isNotBlank()) {
                            text = if (text.isBlank()) clip else text.trimEnd() + "\n" + clip
                        }
                    }
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Paste from clipboard")
                }
                ImportPreview(parsed = parsed, matchingList = matchingList)
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(text) }, enabled = canImport) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ImportPreview(parsed: ParsedBulkImport?, matchingList: String?) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            when {
                parsed == null -> PreviewLine("Nothing to import yet.")
                parsed.items.isEmpty() -> PreviewLine("Add at least one item below “${parsed.title}”.")
                else -> {
                    Text(
                        text = if (matchingList != null) "Adds to “$matchingList”" else "Creates “${parsed.title}”",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val details = buildList {
                        add(pluralize(parsed.items.size, "item"))
                        if (parsed.duplicates.isNotEmpty()) add("${parsed.duplicates.size} repeated, will be skipped")
                    }
                    PreviewLine(details.joinToString(" · "))
                }
            }
        }
    }
}

@Composable
private fun PreviewLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Shown after an import that skipped some lines, listing each one and why. */
@Composable
fun ImportSummaryDialog(
    result: BulkImportResult,
    onOpenList: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (result.createdList) "Created “${result.listName}”" else "Updated “${result.listName}”") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Added ${pluralize(result.addedCount, "item")}. " +
                        "${pluralize(result.skipped.size, "line")} skipped:",
                    style = MaterialTheme.typography.bodyMedium
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(result.skipped) { skipped ->
                        Column {
                            Text(
                                text = skipped.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (skipped.reason) {
                                    SkipReason.DUPLICATE_IN_TEXT -> "Repeated in the pasted text"
                                    SkipReason.ALREADY_IN_LIST -> "Already in this list"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenList) { Text("Open list") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
