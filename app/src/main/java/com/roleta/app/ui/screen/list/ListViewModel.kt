package com.roleta.app.ui.screen.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roleta.app.data.datastore.SortOrder
import com.roleta.app.data.db.entity.ItemEntity
import com.roleta.app.data.repository.RoletaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ListEvent {
    data class ShowToast(val message: String) : ListEvent()
    data object NavigateBack : ListEvent()
}

data class ListUiState(
    val items: List<ItemEntity> = emptyList(),
    val sortOrder: SortOrder = SortOrder.ALPHA,
    val showAddDialog: Boolean = false,
    val addDialogError: String? = null,
    val editTarget: ItemEntity? = null,
    val editDialogError: String? = null,
    val showRenameListDialog: Boolean = false,
    val renameListError: String? = null,
    val showDeleteListDialog: Boolean = false,
    val showExportEmptyPrompt: Boolean = false,
    val pendingImportLines: List<String>? = null,
    val showImportConfirmDialog: Boolean = false
)

@HiltViewModel
class ListViewModel @Inject constructor(
    private val repository: RoletaRepository
) : ViewModel() {

    private lateinit var listId: String
    private lateinit var listName: String

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ListEvent>()
    val events: SharedFlow<ListEvent> = _events.asSharedFlow()

    fun init(listId: String, listName: String) {
        if (this::listId.isInitialized) return
        this.listId = listId
        this.listName = listName
        viewModelScope.launch {
            repository.getActiveItems(listId).collect { items ->
                _uiState.value = _uiState.value.copy(items = items)
            }
        }
        viewModelScope.launch {
            repository.itemSortOrder.collect { sort ->
                _uiState.value = _uiState.value.copy(sortOrder = sort)
            }
        }
    }

    fun toggleSortOrder() {
        viewModelScope.launch {
            val next = if (_uiState.value.sortOrder == SortOrder.ALPHA) SortOrder.CREATION else SortOrder.ALPHA
            repository.setItemSortOrder(next)
        }
    }

    // ── Items ─────────────────────────────────────────────────────────────────

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true, addDialogError = null)
    }

    fun dismissAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, addDialogError = null)
    }

    fun addItem(text: String) {
        viewModelScope.launch {
            val error = repository.addItem(listId, text)
            if (error == null) {
                _uiState.value = _uiState.value.copy(showAddDialog = false, addDialogError = null)
            } else {
                _uiState.value = _uiState.value.copy(addDialogError = error)
            }
        }
    }

    fun openEditDialog(item: ItemEntity) {
        _uiState.value = _uiState.value.copy(editTarget = item, editDialogError = null)
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(editTarget = null, editDialogError = null)
    }

    fun editItem(newText: String) {
        val item = _uiState.value.editTarget ?: return
        viewModelScope.launch {
            val error = repository.editItem(item.id, listId, newText)
            if (error == null) {
                _uiState.value = _uiState.value.copy(editTarget = null, editDialogError = null)
            } else {
                _uiState.value = _uiState.value.copy(editDialogError = error)
            }
        }
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item.id)
        }
    }

    fun pickItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.acceptPick(item.id, listId)
            _events.emit(ListEvent.ShowToast("\"${item.text}\" picked!"))
        }
    }

    // ── List settings ─────────────────────────────────────────────────────────

    fun openRenameListDialog() {
        _uiState.value = _uiState.value.copy(showRenameListDialog = true, renameListError = null)
    }

    fun dismissRenameListDialog() {
        _uiState.value = _uiState.value.copy(showRenameListDialog = false, renameListError = null)
    }

    fun renameList(newName: String) {
        viewModelScope.launch {
            val error = repository.renameList(listId, newName)
            if (error == null) {
                this@ListViewModel.listName = newName.trim()
                _uiState.value = _uiState.value.copy(showRenameListDialog = false, renameListError = null)
            } else {
                _uiState.value = _uiState.value.copy(renameListError = error)
            }
        }
    }

    fun openDeleteListDialog() {
        _uiState.value = _uiState.value.copy(showDeleteListDialog = true)
    }

    fun dismissDeleteListDialog() {
        _uiState.value = _uiState.value.copy(showDeleteListDialog = false)
    }

    fun confirmDeleteList() {
        viewModelScope.launch {
            repository.deleteList(listId)
            _events.emit(ListEvent.NavigateBack)
        }
    }

    // ── Export ────────────────────────────────────────────────────────────────

    private var cachedExportLines: List<String>? = null

    /** Returns true if there are items to export; false if the list is empty (shows dialog). */
    suspend fun prepareExport(): Boolean {
        val lines = repository.exportItems(listId)
        return if (lines.isEmpty()) {
            _uiState.value = _uiState.value.copy(showExportEmptyPrompt = true)
            cachedExportLines = null
            false
        } else {
            cachedExportLines = lines
            true
        }
    }

    /** Retrieves the pre-fetched export lines and clears the cache. */
    fun consumeExportLines(): List<String>? {
        val lines = cachedExportLines
        cachedExportLines = null
        return lines
    }

    fun dismissExportEmptyPrompt() {
        _uiState.value = _uiState.value.copy(showExportEmptyPrompt = false)
    }

    // ── Import ────────────────────────────────────────────────────────────────

    fun onFilePicked(lines: List<String>) {
        val hasActiveItems = _uiState.value.items.isNotEmpty()
        if (hasActiveItems) {
            _uiState.value = _uiState.value.copy(
                pendingImportLines = lines,
                showImportConfirmDialog = true
            )
        } else {
            performImport(lines)
        }
    }

    fun confirmImport() {
        val lines = _uiState.value.pendingImportLines ?: return
        _uiState.value = _uiState.value.copy(pendingImportLines = null, showImportConfirmDialog = false)
        performImport(lines)
    }

    fun dismissImportDialog() {
        _uiState.value = _uiState.value.copy(pendingImportLines = null, showImportConfirmDialog = false)
    }

    private fun performImport(lines: List<String>) {
        viewModelScope.launch {
            val error = repository.importItems(listId, lines)
            if (error != null) _events.emit(ListEvent.ShowToast(error))
        }
    }

    fun getCurrentListName(): String = listName
}
