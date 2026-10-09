package br.com.studyou.ui.screens.games.crossword

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.CrosswordDirection
import br.com.studyou.domain.model.CrosswordEntry
import br.com.studyou.domain.model.GameType
import br.com.studyou.domain.model.NewGameAttempt
import br.com.studyou.domain.model.displayTitle
import br.com.studyou.domain.repository.FileRepository
import br.com.studyou.domain.repository.GameRepository
import br.com.studyou.ui.screens.files.subjectTitleOf
import br.com.studyou.ui.screens.games.recordFinishedGame
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
    /** Ready once the attempt is recorded; the result screen opens only when the player taps "Finalizar". */
    val pendingResultArgs: GameResultArgs? = null,
    val resultArgs: GameResultArgs? = null
) {
    val currentWord: String get() = selectedIndices.map { letters[it] }.joinToString("")

    /** All cells that belong to the crossword shape, filled or not. */
    val gridCells: Set<Pair<Int, Int>> by lazy {
        entries.flatMap { it.cells() }.toSet()
    }

    /**
     * Crossword-style numbering: each starting cell gets a number, in reading order (top to
     * bottom, left to right). Entries that start on the same cell share the number.
     */
    val startCellNumbers: Map<Pair<Int, Int>, Int> by lazy {
        entries.map { it.row to it.col }
            .distinct()
            .sortedWith(compareBy({ it.first }, { it.second }))
            .withIndex()
            .associate { (index, cell) -> cell to index + 1 }
    }

    fun numberOf(entry: CrosswordEntry): Int = startCellNumbers.getValue(entry.row to entry.col)

    /** Letters already revealed on the grid, from entries the player has already found. */
    val cellLetters: Map<Pair<Int, Int>, Char> by lazy {
        buildMap {
            solvedEntries.forEach { entry -> entry.cells().forEachIndexed { i, cell -> put(cell, entry.answer[i]) } }
        }
    }
}

/** Holds the state of the "Palavras Cruzadas" game for a given study file. */
class CrosswordViewModel(
    private val fileId: String,
    private val fileRepository: FileRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrosswordUiState())
    val uiState: StateFlow<CrosswordUiState> = _uiState.asStateFlow()

    private val startTimeMillis = System.currentTimeMillis()
    private var bonusWords: Set<String> = emptySet()

    // Guards against the last word being confirmed twice and finishing twice.
    private var finished = false

    init {
        viewModelScope.launch {
            val file = fileRepository.observeFiles().first().find { it.id == fileId }
            val puzzle = gameRepository.getCrosswordPuzzle(fileId)
            if (puzzle == null || puzzle.entries.isEmpty()) {
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
        if (state.isComplete || index in state.selectedIndices) return
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
        if (finished) return
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
        if (finished) return
        finished = true
        // The completed grid stays on screen; the result screen only opens on "Finalizar".
        _uiState.value = state.copy(isComplete = true)

        val elapsedSeconds = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
        val timeLabel = "%d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
        val xp = 60 + state.bonusWordsFound.size * 10
        viewModelScope.launch {
            val resultArgs = gameRepository.recordFinishedGame(
                attempt = NewGameAttempt(
                    fileId = fileId,
                    gameType = GameType.CROSSWORD,
                    fileTitle = state.subjectTitle,
                    scoreLabel = "Completo em $timeLabel",
                    // The game only ends once every word on the grid has been found.
                    scorePercent = 100,
                    xpEarned = xp,
                    bestScoreLabel = timeLabel
                ),
                resultArgs = GameResultArgs(
                    fileId = fileId,
                    gameType = GameType.CROSSWORD,
                    title = "${GameType.CROSSWORD.displayTitle()} — ${state.subjectTitle}",
                    big = timeLabel,
                    caption = "para completar",
                    xp = xp,
                    streak = 0,
                    note = if (state.bonusWordsFound.isEmpty()) {
                        "Você completou as palavras cruzadas!"
                    } else {
                        "Você completou as palavras cruzadas e encontrou ${state.bonusWordsFound.size} palavra(s) bônus: " +
                            state.bonusWordsFound.joinToString(", ")
                    }
                )
            )
            _uiState.value = _uiState.value.copy(pendingResultArgs = resultArgs)
        }
    }

    /** Leaves the completed grid for the result screen. */
    fun onFinishClick() {
        val state = _uiState.value
        val pending = state.pendingResultArgs ?: return
        _uiState.value = state.copy(resultArgs = pending)
    }
}
