package com.roleta.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roleta.app.data.db.dao.ListWithCount
import com.roleta.app.ui.component.DeleteListDialog
import com.roleta.app.ui.component.EmptyState
import com.roleta.app.ui.component.SectionHeader
import com.roleta.app.ui.component.TextInputDialog
import com.roleta.app.ui.component.groupedItemShape
import com.roleta.app.ui.component.pluralize
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToList: (listId: String, listName: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNewListSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigateToList.collect { (listId, listName) ->
            onNavigateToList(listId, listName)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.Imported -> {
                    val r = event.result
                    val message = if (r.createdList) {
                        "Created “${r.listName}” with ${pluralize(r.addedCount, "item")}"
                    } else {
                        "Added ${pluralize(r.addedCount, "item")} to “${r.listName}”"
                    }
                    val action = snackbarHostState.showSnackbar(message, actionLabel = "Open")
                    if (action == SnackbarResult.ActionPerformed) onNavigateToList(r.listId, r.listName)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Roleta") },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!state.isLoading && state.lists.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showNewListSheet = true },
                    expanded = listState.firstVisibleItemIndex == 0,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New list") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Unit
                state.lists.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    title = "No lists yet",
                    subtitle = "Make a list of anything — places to eat, movies, trips — and let Roleta pick for you.",
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = { viewModel.openCreateDialog() }) { Text("New list") }
                            OutlinedButton(onClick = { viewModel.openImportDialog() }) { Text("Import from text") }
                        }
                    }
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item(key = "header") {
                        SectionHeader(
                            text = pluralize(state.lists.size, "list"),
                            sortOrder = state.sortOrder,
                            onToggleSort = { viewModel.toggleSortOrder() }
                        )
                    }
                    itemsIndexed(state.lists, key = { _, list -> list.id }) { index, list ->
                        ListRow(
                            list = list,
                            index = index,
                            count = state.lists.size,
                            onClick = { onNavigateToList(list.id, list.name) },
                            onRename = { viewModel.openRenameDialog(list) },
                            onDelete = { viewModel.openDeleteDialog(list) }
                        )
                    }
                }
            }
        }
    }

    if (showNewListSheet) {
        NewListSheet(
            onNewList = { viewModel.openCreateDialog() },
            onImport = { viewModel.openImportDialog() },
            onDismiss = { showNewListSheet = false }
        )
    }

    if (state.showCreateDialog) {
        TextInputDialog(
            title = "New List",
            label = "List Name",
            confirmText = "Create",
            errorMessage = state.createDialogError,
            onConfirm = { viewModel.createList(it) },
            onDismiss = { viewModel.dismissCreateDialog() }
        )
    }

    if (state.showRenameDialog && state.renameTarget != null) {
        TextInputDialog(
            title = "Rename List",
            label = "List Name",
            initialValue = state.renameTarget!!.name,
            confirmText = "Rename",
            errorMessage = state.renameDialogError,
            onConfirm = { viewModel.renameList(it) },
            onDismiss = { viewModel.dismissRenameDialog() }
        )
    }

    if (state.showDeleteDialog && state.deleteTarget != null) {
        DeleteListDialog(
            listName = state.deleteTarget!!.name,
            onConfirm = { viewModel.confirmDeleteList() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }

    if (state.showImportDialog) {
        BulkImportDialog(
            existingListNames = state.lists.map { it.name },
            isImporting = state.isImporting,
            onImport = { viewModel.importFromText(it) },
            onDismiss = { viewModel.dismissImportDialog() }
        )
    }

    state.importSummary?.let { summary ->
        ImportSummaryDialog(
            result = summary,
            onOpenList = { viewModel.openImportedList() },
            onDismiss = { viewModel.dismissImportSummary() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewListSheet(
    onNewList: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Hide the sheet with its animation, then run the chosen action.
    fun choose(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Add a list",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            SheetOption(
                icon = { Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = null) },
                title = "New list",
                subtitle = "Start empty and add items one at a time",
                onClick = { choose(onNewList) }
            )
            SheetOption(
                icon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null) },
                title = "Import from text",
                subtitle = "Paste a title and items, one per line",
                onClick = { choose(onImport) }
            )
        }
    }
}

@Composable
private fun SheetOption(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = icon,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    )
}

@Composable
private fun ListRow(
    list: ListWithCount,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = groupedItemShape(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Monogram(list.name)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pluralize(list.activeCount, "item"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options for ${list.name}")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onRename() }
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

/** First letter of the list name in a tinted circle. */
@Composable
private fun Monogram(name: String) {
    val letter = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "#"
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
