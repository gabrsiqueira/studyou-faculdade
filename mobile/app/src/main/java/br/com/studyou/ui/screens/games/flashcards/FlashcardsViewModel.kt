package br.com.studyou.ui.screens.games.flashcards

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.Flashcard
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

@Immutable
data class FlashcardsUiState(
    val subjectTitle: String = "",
    val cards: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val rememberedCount: Int = 0,
    val isLoading: Boolean = true,
    val resultArgs: GameResultArgs? = null
) {
    val currentCard: Flashcard? get() = cards.getOrNull(currentIndex)
    val progressLabel: String get() = "Cartão ${currentIndex + 1} de ${cards.size}"
}

/** Holds the state of the Flashcards game for a given study file. */
class FlashcardsViewModel(
    private val fileId: String,
    private val fileRepository: FileRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardsUiState())
    val uiState: StateFlow<FlashcardsUiState> = _uiState.asStateFlow()

    // Guards against "Lembrei" on the last card being tapped twice and finishing twice.
    private var finished = false

    init {
        viewModelScope.launch {
            val file = fileRepository.observeFiles().first().find { it.id == fileId }
            val cards = gameRepository.getFlashcards(fileId)
            _uiState.value = FlashcardsUiState(
                subjectTitle = subjectTitleOf(file?.name.orEmpty()),
                cards = cards,
                isLoading = false
            )
        }
    }

    fun flipCard() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun markCard(remembered: Boolean) {
        if (finished) return
        val state = _uiState.value
        val newRememberedCount = if (remembered) state.rememberedCount + 1 else state.rememberedCount
        val isLastCard = state.currentIndex == state.cards.lastIndex

        if (isLastCard) {
            finish(state.copy(rememberedCount = newRememberedCount))
        } else {
            _uiState.value = state.copy(
                currentIndex = state.currentIndex + 1,
                isFlipped = false,
                rememberedCount = newRememberedCount
            )
        }
    }

    private fun finish(state: FlashcardsUiState) {
        if (finished) return
        finished = true
        _uiState.value = state

        val total = state.cards.size
        val percent = if (total == 0) 0 else (state.rememberedCount * 100) / total
        val xp = state.rememberedCount * 8
        viewModelScope.launch {
            val resultArgs = gameRepository.recordFinishedGame(
                attempt = NewGameAttempt(
                    fileId = fileId,
                    gameType = GameType.FLASHCARDS,
                    fileTitle = state.subjectTitle,
                    scoreLabel = "$percent% lembrados",
                    scorePercent = percent,
                    xpEarned = xp,
                    bestScoreLabel = "$percent% lembrados"
                ),
                resultArgs = GameResultArgs(
                    fileId = fileId,
                    gameType = GameType.FLASHCARDS,
                    title = "${GameType.FLASHCARDS.displayTitle()} — ${state.subjectTitle}",
                    big = "$percent%",
                    caption = "lembrados",
                    xp = xp,
                    streak = 0,
                    note = "Você revisou $total cartões e lembrou de ${state.rememberedCount} deles."
                )
            )
            _uiState.value = _uiState.value.copy(resultArgs = resultArgs)
        }
    }
}
