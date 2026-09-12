package com.roleta.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roleta.app.data.datastore.SortOrder
import com.roleta.app.data.db.dao.ListWithCount
import com.roleta.app.data.repository.CreateListResult
import com.roleta.app.data.repository.RoletaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val lists: List<ListWithCount> = emptyList(),
    val sortOrder: SortOrder = SortOrder.ALPHA,
    val showCreateDialog: Boolean = false,
    val createDialogError: String? = null,
    val showRenameDialog: Boolean = false,
    val renameTarget: ListWithCount? = null,
    val renameDialogError: String? = null,
    val showDeleteDialog: Boolean = false,
    val deleteTarget: ListWithCount? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: RoletaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigateToList = MutableSharedFlow<Pair<String, String>>()
    val navigateToList: SharedFlow<Pair<String, String>> = _navigateToList.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getLists().collect { lists ->
                _uiState.value = _uiState.value.copy(lists = lists)
            }
        }
        viewModelScope.launch {
            repository.listSortOrder.collect { sort ->
                _uiState.value = _uiState.value.copy(sortOrder = sort)
            }
        }
        viewModelScope.launch {
            repository.hasLaunched.collect { launched ->
                if (!launched) repository.seedSampleList()
            }
        }
    }

    fun toggleSortOrder() {
        viewModelScope.launch {
            val next = if (_uiState.value.sortOrder == SortOrder.ALPHA) SortOrder.CREATION else SortOrder.ALPHA
            repository.setListSortOrder(next)
        }
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true, createDialogError = null)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false, createDialogError = null)
    }

    fun createList(name: String) {
        viewModelScope.launch {
            when (val result = repository.createList(name)) {
                is CreateListResult.Success -> {
                    _uiState.value = _uiState.value.copy(showCreateDialog = false, createDialogError = null)
                    _navigateToList.emit(result.id to name.trim())
                }
                is CreateListResult.Error -> {
                    _uiState.value = _uiState.value.copy(createDialogError = result.message)
                }
            }
        }
    }

    fun openRenameDialog(list: ListWithCount) {
        _uiState.value = _uiState.value.copy(showRenameDialog = true, renameTarget = list, renameDialogError = null)
    }

    fun dismissRenameDialog() {
        _uiState.value = _uiState.value.copy(showRenameDialog = false, renameTarget = null, renameDialogError = null)
    }

    fun renameList(newName: String) {
        val target = _uiState.value.renameTarget ?: return
        viewModelScope.launch {
            val error = repository.renameList(target.id, newName)
            if (error == null) {
                _uiState.value = _uiState.value.copy(showRenameDialog = false, renameTarget = null, renameDialogError = null)
            } else {
                _uiState.value = _uiState.value.copy(renameDialogError = error)
            }
        }
    }

    fun openDeleteDialog(list: ListWithCount) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true, deleteTarget = list)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false, deleteTarget = null)
    }

    fun confirmDeleteList() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            repository.deleteList(target.id)
            _uiState.value = _uiState.value.copy(showDeleteDialog = false, deleteTarget = null)
        }
    }
}
