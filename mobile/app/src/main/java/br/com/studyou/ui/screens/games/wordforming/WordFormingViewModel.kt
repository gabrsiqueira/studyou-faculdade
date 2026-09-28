package br.com.studyou.ui.screens.games.wordforming

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.GameAttempt
import br.com.studyou.domain.model.GameType
import br.com.studyou.domain.repository.FileRepository
import br.com.studyou.domain.repository.GameRepository
import br.com.studyou.ui.screens.files.subjectTitleOf
import br.com.studyou.ui.screens.result.GameResultArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Immutable
data class WordFormingUiState(
    val subjectTitle: String = "",
    val letters: List<Char> = emptyList(),
    val selectedIndices: List<Int> = emptyList(),
    val foundWords: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isUnavailable: Boolean = false,
    val resultArgs: GameResultArgs? = null
) {
    val currentWord: String get() = selectedIndices.map { letters[it] }.joinToString("")
}

/** Holds the state of the "Forma-Palavras" game for a given study file. */
class WordFormingViewModel(
    private val fileId: String,
    private val fileRepository: FileRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordFormingUiState())
    val uiState: StateFlow<WordFormingUiState> = _uiState.asStateFlow()

    private var validWords: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            val file = fileRepository.observeFiles().first().find { it.id == fileId }
            val puzzle = gameRepository.getWordFormingPuzzle(fileId)
            if (puzzle == null || puzzle.letters.isEmpty() || puzzle.validWords.isEmpty()) {
                _uiState.value = WordFormingUiState(
                    subjectTitle = subjectTitleOf(file?.name.orEmpty()),
                    isLoading = false,
                    isUnavailable = true
                )
                return@launch
            }
            validWords = puzzle.validWords
            _uiState.value = WordFormingUiState(
                subjectTitle = subjectTitleOf(file?.name.orEmpty()),
                letters = puzzle.letters,
                isLoading = false
            )
        }
    }

    fun onLetterTap(index: Int) {
        val state = _uiState.value
        if (index in state.selectedIndices) return
        _uiState.value = state.copy(selectedIndices = state.selectedIndices + index)
    }

    fun onClear() {
        _uiState.value = _uiState.value.copy(selectedIndices = emptyList())
    }

    fun onShuffle() {
        val state = _uiState.value
        _uiState.value = state.copy(letters = state.letters.shuffled(), selectedIndices = emptyList())
    }

    fun onConfirmWord() {
        val state = _uiState.value
        val word = state.currentWord
        val isNewValidWord = word.length >= 3 && word in validWords && word !in state.foundWords
        _uiState.value = state.copy(
            foundWords = if (isNewValidWord) state.foundWords + word else state.foundWords,
            selectedIndices = emptyList()
        )
    }

    fun onFinishGame() {
        val state = _uiState.value
        val xp = state.foundWords.size * 15
        viewModelScope.launch {
            gameRepository.saveAttempt(
                GameAttempt(
                    id = "word-forming-$fileId-${System.currentTimeMillis()}",
                    gameType = GameType.WORD_FORMING,
                    fileTitle = state.subjectTitle,
                    playedAtLabel = "hoje",
                    scoreLabel = "${state.foundWords.size} palavras",
                    xpEarned = xp,
                    streakDays = 0,
                    bestScoreLabel = "${state.foundWords.size} palavras"
                )
            )
        }
        _uiState.value = state.copy(
            resultArgs = GameResultArgs(
                fileId = fileId,
                gameType = GameType.WORD_FORMING,
                title = "Forma-Palavras — ${state.subjectTitle}",
                big = "${state.foundWords.size}",
                caption = "palavras encontradas",
                xp = xp,
                streak = 0,
                note = "Palavras encontradas: ${state.foundWords.joinToString(", ")}"
            )
        )
    }
}
