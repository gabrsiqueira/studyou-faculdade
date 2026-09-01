package br.com.studyou.ui.screens.games.guessword

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

enum class LetterState { CORRECT, PRESENT, ABSENT }

/** Compares [guess] against [answer], returning the Wordle-style state of each letter. */
fun evaluateGuess(guess: String, answer: String): List<LetterState> {
    val states = MutableList(guess.length) { LetterState.ABSENT }
    val remaining = answer.toMutableList()

    for (i in guess.indices) {
        if (i < answer.length && guess[i] == answer[i]) {
            states[i] = LetterState.CORRECT
            remaining[i] = '#'
        }
    }
    for (i in guess.indices) {
        if (states[i] == LetterState.CORRECT) continue
        val foundAt = remaining.indexOf(guess[i])
        if (foundAt >= 0) {
            states[i] = LetterState.PRESENT
            remaining[foundAt] = '#'
        }
    }
    return states
}

private fun LetterState.priority() = when (this) {
    LetterState.CORRECT -> 2
    LetterState.PRESENT -> 1
    LetterState.ABSENT -> 0
}

@Immutable
data class GuessWordUiState(
    val subjectTitle: String = "",
    val answerLength: Int = 0,
    val maxAttempts: Int = 6,
    val guesses: List<List<Pair<Char, LetterState>>> = emptyList(),
    val currentInput: String = "",
    val keyboardStates: Map<Char, LetterState> = emptyMap(),
    val isLoading: Boolean = true,
    val isUnavailable: Boolean = false,
    val isSolved: Boolean = false,
    val isFailed: Boolean = false,
    val resultArgs: GameResultArgs? = null
) {
    val attemptsUsed: Int get() = guesses.size
    val attemptLabel: String get() = "Tentativa ${(attemptsUsed + 1).coerceAtMost(maxAttempts)} de $maxAttempts"
}

/** Holds the state of the "Termo" (guess the word) game for a given study file. */
class GuessWordViewModel(
    private val fileId: String,
    private val fileRepository: FileRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuessWordUiState())
    val uiState: StateFlow<GuessWordUiState> = _uiState.asStateFlow()

    private lateinit var answer: String

    init {
        viewModelScope.launch {
            val file = fileRepository.observeFiles().first().find { it.id == fileId }
            val puzzle = gameRepository.getGuessWordPuzzle(fileId)
            if (puzzle == null) {
                _uiState.value = GuessWordUiState(
                    subjectTitle = subjectTitleOf(file?.name.orEmpty()),
                    isLoading = false,
                    isUnavailable = true
                )
                return@launch
            }
            answer = puzzle.answer.uppercase()
            _uiState.value = GuessWordUiState(
                subjectTitle = subjectTitleOf(file?.name.orEmpty()),
                answerLength = answer.length,
                maxAttempts = puzzle.maxAttempts,
                isLoading = false
            )
        }
    }

    fun onLetterTyped(letter: Char) {
        val state = _uiState.value
        if (state.currentInput.length >= state.answerLength) return
        _uiState.value = state.copy(currentInput = state.currentInput + letter.uppercaseChar())
    }

    fun onBackspace() {
        val state = _uiState.value
        if (state.currentInput.isEmpty()) return
        _uiState.value = state.copy(currentInput = state.currentInput.dropLast(1))
    }

    fun onSubmitGuess() {
        val state = _uiState.value
        if (state.currentInput.length != state.answerLength) return

        val evaluation = evaluateGuess(state.currentInput, answer)
        val guessRow = state.currentInput.toList().zip(evaluation)
        val updatedKeyboard = state.keyboardStates.toMutableMap()
        guessRow.forEach { (letter, letterState) ->
            val existing = updatedKeyboard[letter]
            if (existing == null || letterState.priority() > existing.priority()) {
                updatedKeyboard[letter] = letterState
            }
        }

        val solved = evaluation.all { it == LetterState.CORRECT }
        val newGuesses = state.guesses + listOf(guessRow)
        val failed = !solved && newGuesses.size >= state.maxAttempts

        val newState = state.copy(
            guesses = newGuesses,
            currentInput = "",
            keyboardStates = updatedKeyboard,
            isSolved = solved,
            isFailed = failed
        )

        if (solved || failed) finish(newState, solved) else _uiState.value = newState
    }

    private fun finish(state: GuessWordUiState, solved: Boolean) {
        val attempts = state.guesses.size
        val xp = if (solved) (state.maxAttempts - attempts + 1) * 15 else 0
        viewModelScope.launch {
            gameRepository.saveAttempt(
                GameAttempt(
                    id = "guess-$fileId-${System.currentTimeMillis()}",
                    gameType = GameType.GUESS_WORD,
                    fileTitle = state.subjectTitle,
                    playedAtLabel = "hoje",
                    scoreLabel = "$attempts tentativas",
                    xpEarned = xp,
                    streakDays = 5,
                    bestScoreLabel = "$attempts tentativas"
                )
            )
        }
        _uiState.value = state.copy(
            resultArgs = GameResultArgs(
                fileId = fileId,
                gameType = GameType.GUESS_WORD,
                title = "Termo — ${state.subjectTitle}",
                big = if (solved) "$attempts tentativas" else "Não foi dessa vez",
                caption = if (solved) "para acertar" else "A palavra era \"$answer\"",
                xp = xp,
                streak = 5,
                note = if (solved) "Você descobriu a palavra-chave!" else "Tente novamente para descobrir a palavra."
            )
        )
    }
}
