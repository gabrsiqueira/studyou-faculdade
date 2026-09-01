package br.com.studyou.ui.screens.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.GameHistoryGroup
import br.com.studyou.domain.model.GameType
import br.com.studyou.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
data class HistoryUiState(
    val groups: List<GameHistoryGroup> = emptyList(),
    val expandedGameType: GameType? = null
)

/** Holds the state of the "Meu histórico" screen. */
class HistoryViewModel(historyRepository: HistoryRepository) : ViewModel() {

    private val expandedGameType = MutableStateFlow<GameType?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        historyRepository.observeHistory(),
        expandedGameType
    ) { groups, expanded ->
        HistoryUiState(groups = groups, expandedGameType = expanded)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onToggleExpand(gameType: GameType) {
        expandedGameType.value = if (expandedGameType.value == gameType) null else gameType
    }
}
