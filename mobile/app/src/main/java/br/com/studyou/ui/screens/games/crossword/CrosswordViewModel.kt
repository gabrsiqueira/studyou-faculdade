package br.com.studyou.ui.screens.games.crossword

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.CrosswordDirection
import br.com.studyou.domain.model.CrosswordEntry
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

/** The (row, col) cells an entry occupies on the grid, in order. */
fun CrosswordEntry.cells(): List<Pair<Int, Int>> = answer.indices.map { offset ->
    if (direction == CrosswordDirection.ACROSS) row to col + offset else row + offset to col
}

@Immutable
data class CrosswordUiState(
    val subjectTitle: String = "",
    val size: Int = 0,
    val letters: List<Char> = emptyList(),
    val selectedIndices: List<Int> = emptyList(),
    val entries: List<CrosswordEntry> = emptyList(),
    val solvedEntries: Set<CrosswordEntry> = emptySet(),
    val bonusWordsFound: List<String> = emptyList(),
    val lastAttemptCorrect: Boolean? = null,
    val isLoading: Boolean = true,
    val isUnavailable: Boolean = false,
    val isComplete: Boolean = false,
    val resultArgs: GameResultArgs? = null
) {
    val currentWord: String get() = selectedIndices.map { letters[it] }.joinToString("")

    /** All cells that belong to the crossword shape, filled or not. */
    val gridCells: Set<Pair<Int, Int>> by lazy {
        entries.flatMap { it.cells() }.toSet()
    }

    /** Letters already revealed on the grid, from entries the player has already found. */
    val cellLetters: Map<Pair<Int, Int>, Char> by lazy {
        buildMap {
            solvedEntries.forEach { entry -> entry.cells().forEachIndexed { i, cell -> put(cell, entry.answer[i]) } }
        }
    }
}

/** Holds the state of the "Cruzamento" game for a given study file. */
class CrosswordViewModel(
    private val fileId: String,
    private val fileRepository: FileRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrosswordUiState())
    val uiState: StateFlow<CrosswordUiState> = _uiState.asStateFlow()

    private val startTimeMillis = System.currentTimeMillis()
    private var bonusWords: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            val file = fileRepository.observeFiles().first().find { it.id == fileId }
            val puzzle = gameRepository.getCrosswordPuzzle(fileId)
            if (puzzle == null) {
                _uiState.value = CrosswordUiState(
                    subjectTitle = subjectTitleOf(file?.name.orEmpty()),
                    isLoading = false,
                    isUnavailable = true
                )
                return@launch
            }
            bonusWords = puzzle.bonusWords
            _uiState.value = CrosswordUiState(
                subjectTitle = subjectTitleOf(file?.name.orEmpty()),
                size = puzzle.size,
                letters = puzzle.letters,
                entries = puzzle.entries,
                isLoading = false
            )
        }
    }

    fun onLetterTap(index: Int) {
        val state = _uiState.value
        if (index in state.selectedIndices) return
        _uiState.value = state.copy(selectedIndices = state.selectedIndices + index, lastAttemptCorrect = null)
    }

    fun onClear() {
        _uiState.value = _uiState.value.copy(selectedIndices = emptyList(), lastAttemptCorrect = null)
    }

    fun onShuffle() {
        val state = _uiState.value
        _uiState.value = state.copy(letters = state.letters.shuffled(), selectedIndices = emptyList())
    }

    fun onConfirmWord() {
        val state = _uiState.value
        val word = state.currentWord
        if (word.isEmpty()) return

        val matchedEntry = state.entries.firstOrNull { it !in state.solvedEntries && it.answer == word }
        when {
            matchedEntry != null -> {
                val solved = state.solvedEntries + matchedEntry
                val newState = state.copy(
                    solvedEntries = solved,
                    selectedIndices = emptyList(),
                    lastAttemptCorrect = true
                )
                if (solved.size == state.entries.size) finish(newState) else _uiState.value = newState
            }

            word.length >= 3 && word in bonusWords && word !in state.bonusWordsFound -> {
                _uiState.value = state.copy(
                    bonusWordsFound = state.bonusWordsFound + word,
                    selectedIndices = emptyList(),
                    lastAttemptCorrect = true
                )
            }

            else -> {
                _uiState.value = state.copy(selectedIndices = emptyList(), lastAttemptCorrect = false)
            }
        }
    }

    private fun finish(state: CrosswordUiState) {
        val elapsedSeconds = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
        val timeLabel = "%d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
        val xp = 60 + state.bonusWordsFound.size * 10
        viewModelScope.launch {
            gameRepository.saveAttempt(
                GameAttempt(
                    id = "crossword-$fileId-${System.currentTimeMillis()}",
                    gameType = GameType.CROSSWORD,
                    fileTitle = state.subjectTitle,
                    playedAtLabel = "hoje",
                    scoreLabel = "Completo em $timeLabel",
                    xpEarned = xp,
                    streakDays = 5,
                    bestScoreLabel = timeLabel
                )
            )
        }
        _uiState.value = state.copy(
            isComplete = true,
            resultArgs = GameResultArgs(
                fileId = fileId,
                gameType = GameType.CROSSWORD,
                title = "Cruzamento — ${state.subjectTitle}",
                big = timeLabel,
                caption = "para completar",
                xp = xp,
                streak = 5,
                note = if (state.bonusWordsFound.isEmpty()) {
                    "Você completou o cruzamento corretamente!"
                } else {
                    "Você completou o cruzamento e encontrou ${state.bonusWordsFound.size} palavra(s) bônus: " +
                        state.bonusWordsFound.joinToString(", ")
                }
            )
        )
    }
}
