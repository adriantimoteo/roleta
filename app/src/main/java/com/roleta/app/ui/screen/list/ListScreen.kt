package com.roleta.app.ui.screen.list

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roleta.app.data.datastore.SortOrder
import com.roleta.app.data.db.entity.ItemEntity
import com.roleta.app.ui.component.DeleteListDialog
import com.roleta.app.ui.component.EmptyState
import com.roleta.app.ui.screen.home.TextInputDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    listId: String,
    listName: String,
    onNavigateBack: () -> Unit,
    onNavigateToPick: (listId: String, listName: String) -> Unit,
    onNavigateToHistory: (listId: String, listName: String) -> Unit,
    viewModel: ListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overflowExpanded by remember { mutableStateOf(false) }
    // Falls back to the nav-provided name for the single frame before ViewModel.init populates state.
    val currentListName = state.listName.ifBlank { listName }

    LaunchedEffect(listId) {
        viewModel.init(listId, listName)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ListEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is ListEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val lines = viewModel.consumeExportLines() ?: return@rememberLauncherForActivityResult
        scope.launch {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.writer().use { it.write(lines.joinToString("\n")) }
            }
            Toast.makeText(context, "Exported ${lines.size} items.", Toast.LENGTH_SHORT).show()
        }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val lines = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.readLines()
                ?: emptyList()
            viewModel.onFilePicked(lines)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentListName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSortOrder() }) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = if (state.sortOrder == SortOrder.ALPHA) "Sort by creation" else "Sort alphabetically"
                        )
                    }
                    IconButton(onClick = { onNavigateToHistory(listId, currentListName) }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    Box {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import") },
                                onClick = {
                                    overflowExpanded = false
                                    importLauncher.launch(arrayOf("text/plain"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export") },
                                onClick = {
                                    overflowExpanded = false
                                    scope.launch {
                                        if (viewModel.prepareExport()) {
                                            exportLauncher.launch("${currentListName}.txt")
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Rename List") },
                                onClick = { overflowExpanded = false; viewModel.openRenameListDialog() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete List") },
                                onClick = { overflowExpanded = false; viewModel.openDeleteListDialog() }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.items.size >= 2) {
                    FloatingActionButton(onClick = { onNavigateToPick(listId, currentListName) }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Pick random item")
                    }
                }
                FloatingActionButton(onClick = { viewModel.openAddDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.items.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Add,
                    title = "No Items Yet",
                    subtitle = "Tap + to add your first item.",
                    onIconClick = { viewModel.openAddDialog() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            onEdit = { viewModel.openEditDialog(item) },
                            onDelete = { viewModel.deleteItem(item) },
                            onPickThis = { viewModel.pickItem(item) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (state.showAddDialog) {
        TextInputDialog(
            title = "Add Item",
            label = "Item Name",
            confirmText = "Add",
            errorMessage = state.addDialogError,
            onConfirm = { viewModel.addItem(it) },
            onDismiss = { viewModel.dismissAddDialog() }
        )
    }

    if (state.editTarget != null) {
        TextInputDialog(
            title = "Edit Item",
            label = "Item Name",
            initialValue = state.editTarget!!.text,
            confirmText = "Save",
            errorMessage = state.editDialogError,
            onConfirm = { viewModel.editItem(it) },
            onDismiss = { viewModel.dismissEditDialog() }
        )
    }

    if (state.showRenameListDialog) {
        TextInputDialog(
            title = "Rename List",
            label = "List Name",
            initialValue = currentListName,
            confirmText = "Rename",
            errorMessage = state.renameListError,
            onConfirm = { viewModel.renameList(it) },
            onDismiss = { viewModel.dismissRenameListDialog() }
        )
    }

    if (state.showDeleteListDialog) {
        DeleteListDialog(
            listName = currentListName,
            onConfirm = { viewModel.confirmDeleteList() },
            onDismiss = { viewModel.dismissDeleteListDialog() }
        )
    }

    if (state.showExportEmptyPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportEmptyPrompt() },
            title = { Text("Nothing to export") },
            text = { Text("All items have been picked. Add or restore items to export.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissExportEmptyPrompt() }) { Text("OK") }
            }
        )
    }

    if (state.showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportDialog() },
            title = { Text("Replace active items?") },
            text = { Text("This will replace all active items. Pick history will be kept.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport() }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportDialog() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ItemRow(
    item: ItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPickThis: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(item.text) },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Item options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Pick This") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                        onClick = { menuExpanded = false; onPickThis() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    )
}

