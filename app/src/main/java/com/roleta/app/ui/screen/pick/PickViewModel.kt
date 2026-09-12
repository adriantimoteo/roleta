package com.roleta.app.ui.screen.pick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

sealed class PickUiState {
    data object Loading : PickUiState()

    data class Spinning(
        val items: List<String>,
        val targetIndex: Int,
        val spinId: Long,
        val listName: String,
        val quip: String
    ) : PickUiState()

    data class Result(
        val selectedItemText: String,
        val listName: String,
        val priorPickCount: Int?,
        val skipCount: Int = 0
    ) : PickUiState()
}

private val QUIPS = listOf(
    "Hold on to your horses…",
    "Fate is deciding…",
    "The universe is thinking…",
    "Rolling the dice…",
    "Consulting the oracle…",
    "Randomness in progress…",
    "Asking the cosmic algorithm…",
    "This one's on the universe…",
    "Destiny loading…",
    "Let the wheel decide…",
)

@HiltViewModel
class PickViewModel @Inject constructor(
    private val repository: RoletaRepository
) : ViewModel() {

    private lateinit var listId: String
    private lateinit var listName: String
    private var selectedItem: ItemEntity? = null
    private var spinCounter = 0L

    private val _uiState = MutableStateFlow<PickUiState>(PickUiState.Loading)
    val uiState: StateFlow<PickUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    fun init(listId: String, listName: String) {
        if (this::listId.isInitialized) return
        this.listId = listId
        this.listName = listName
        spin()
    }

    fun spin() {
        viewModelScope.launch { doSpin() }
    }

    fun onTryAgain() {
        val itemToSkip = selectedItem
        viewModelScope.launch {
            itemToSkip?.let { repository.skipItem(it.id) }
            doSpin()
        }
    }

    private suspend fun doSpin() {
        val items = repository.getActiveItemsOnce(listId)
        if (items.size < 2) {
            _navigateBack.emit(Unit)
            return
        }
        val pick = items.random()
        selectedItem = pick
        _uiState.value = PickUiState.Spinning(
            items = items.map { it.text },
            targetIndex = items.indexOf(pick),
            spinId = spinCounter++,
            listName = listName,
            quip = QUIPS.random()
        )
    }

    fun onAnimationSettled() {
        viewModelScope.launch {
            val item = selectedItem ?: return@launch
            val history = repository.getPickHistoryForItem(item.id)
            _uiState.value = PickUiState.Result(
                selectedItemText = item.text,
                listName = listName,
                priorPickCount = history?.pickCount,
                skipCount = item.skipCount
            )
        }
    }

    fun onAccept() {
        viewModelScope.launch {
            val item = selectedItem ?: return@launch
            repository.acceptPick(item.id, listId)
            _navigateBack.emit(Unit)
        }
    }

    fun onBack() {
        viewModelScope.launch {
            _navigateBack.emit(Unit)
        }
    }
}
