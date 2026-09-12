package com.roleta.app.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roleta.app.data.db.entity.PickHistoryEntity
import com.roleta.app.data.repository.RestoreResult
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

sealed class HistoryEvent {
    data class ShowToast(val message: String) : HistoryEvent()
}

data class HistoryUiState(
    val entries: List<PickHistoryEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: RoletaRepository
) : ViewModel() {

    private lateinit var listId: String

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events: SharedFlow<HistoryEvent> = _events.asSharedFlow()

    fun init(listId: String) {
        if (this::listId.isInitialized) return
        this.listId = listId
        viewModelScope.launch {
            repository.getHistory(listId).collect { entries ->
                _uiState.value = HistoryUiState(entries = entries, isLoading = false)
            }
        }
    }

    fun restore(entry: PickHistoryEntity) {
        viewModelScope.launch {
            when (repository.restoreItem(entry.id)) {
                RestoreResult.Success ->
                    _events.emit(HistoryEvent.ShowToast("Restored to active list."))
                RestoreResult.AlreadyActive ->
                    _events.emit(HistoryEvent.ShowToast("Already in your active list — history entry removed."))
            }
        }
    }
}
