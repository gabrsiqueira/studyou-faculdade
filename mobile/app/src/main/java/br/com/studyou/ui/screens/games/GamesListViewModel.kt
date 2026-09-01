package br.com.studyou.ui.screens.games

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.GameSummary
import br.com.studyou.domain.repository.FileRepository
import br.com.studyou.domain.repository.GameRepository
import br.com.studyou.ui.screens.files.subjectTitleOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Immutable
data class GamesListUiState(
    val fileName: String = "",
    val games: List<GameSummary> = emptyList(),
    val isLoading: Boolean = true
)

/** Holds the state of the "Jogos disponíveis" screen for a given study file. */
class GamesListViewModel(
    private val fileId: String,
    private val fileRepository: FileRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamesListUiState())
    val uiState: StateFlow<GamesListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val file = fileRepository.observeFiles().first().find { it.id == fileId }
            val games = gameRepository.getGames(fileId)
            _uiState.value = GamesListUiState(
                fileName = file?.name.orEmpty(),
                games = games,
                isLoading = false
            )
        }
    }
}

fun GamesListUiState.subjectTitle(): String = subjectTitleOf(fileName)
