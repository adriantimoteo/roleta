package com.roleta.app.ui.screen.list

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roleta.app.data.db.entity.ItemEntity
import com.roleta.app.ui.component.DeleteListDialog
import com.roleta.app.ui.component.EmptyState
import com.roleta.app.ui.component.SectionHeader
import com.roleta.app.ui.component.TextInputDialog
import com.roleta.app.ui.component.groupedItemShape
import com.roleta.app.ui.component.pluralize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var overflowExpanded by remember { mutableStateOf(false) }
    // Falls back to the nav-provided name for the single frame before ViewModel.init populates state.
    val currentListName = state.listName.ifBlank { listName }
    val canSpin = state.items.size >= 2

    LaunchedEffect(listId) {
        viewModel.init(listId, listName)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ListEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
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
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.writer().use { it.write(lines.joinToString("\n")) }
                }
            }
            snackbarHostState.showSnackbar("Exported ${pluralize(lines.size, "item")}")
        }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val lines = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readLines() }
                    ?: emptyList()
            }
            viewModel.onFilePicked(lines)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(currentListName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToHistory(listId, currentListName) }) {
                        Icon(Icons.Outlined.History, contentDescription = "History")
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
                                text = { Text("Import from file") },
                                leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                                onClick = {
                                    overflowExpanded = false
                                    importLauncher.launch(arrayOf("text/plain"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export to file") },
                                leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = null) },
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
                                text = { Text("Rename list") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = { overflowExpanded = false; viewModel.openRenameListDialog() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete list", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { overflowExpanded = false; viewModel.openDeleteListDialog() }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!state.isLoading && state.items.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (canSpin) {
                        SmallFloatingActionButton(
                            onClick = { viewModel.openAddDialog() },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add item")
                        }
                        ExtendedFloatingActionButton(
                            onClick = { onNavigateToPick(listId, currentListName) },
                            icon = { Icon(Icons.Default.Casino, contentDescription = null) },
                            text = { Text("Spin") }
                        )
                    } else {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openAddDialog() },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Add item") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> Unit
                state.items.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    title = "No items yet",
                    subtitle = "Add at least two items, then spin to let Roleta choose.",
                    action = {
                        FilledTonalButton(onClick = { viewModel.openAddDialog() }) { Text("Add item") }
                    }
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item(key = "header") {
                        SectionHeader(
                            text = if (canSpin) pluralize(state.items.size, "item")
                            else "1 item · add one more to spin",
                            sortOrder = state.sortOrder,
                            onToggleSort = { viewModel.toggleSortOrder() }
                        )
                    }
                    itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                        ItemRow(
                            item = item,
                            index = index,
                            count = state.items.size,
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
    index: Int,
    count: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPickThis: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        onClick = onEdit,
        shape = groupedItemShape(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(start = 20.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options for ${item.text}")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Pick this") },
                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                        onClick = { menuExpanded = false; onPickThis() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}
