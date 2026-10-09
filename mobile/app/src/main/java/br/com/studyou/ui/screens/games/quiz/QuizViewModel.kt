package br.com.studyou.ui.screens.games.quiz

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.GameType
import br.com.studyou.domain.model.NewGameAttempt
import br.com.studyou.domain.model.displayTitle
import br.com.studyou.domain.model.QuizQuestion
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
data class QuizUiState(
    val subjectTitle: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val correctCount: Int = 0,
    val isLoading: Boolean = true,
    val resultArgs: GameResultArgs? = null
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val progressLabel: String get() = "Pergunta ${currentIndex + 1} de ${questions.size}"
    val progress: Float get() = if (questions.isEmpty()) 0f else (currentIndex + 1) / questions.size.toFloat()
}

/** Holds the state of the Quiz game for a given study file. */
class QuizViewModel(
    private val fileId: String,
    private val fileRepository: FileRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    // Guards against the last answer being confirmed twice (double tap) and finishing twice.
    private var finished = false

    init {
        viewModelScope.launch {
            val file = fileRepository.observeFiles().first().find { it.id == fileId }
            val questions = gameRepository.getQuiz(fileId)
            _uiState.value = QuizUiState(
                subjectTitle = subjectTitleOf(file?.name.orEmpty()),
                questions = questions,
                isLoading = false
            )
        }
    }

    fun selectOption(index: Int) {
        _uiState.value = _uiState.value.copy(selectedOptionIndex = index)
    }

    fun confirmAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        val selected = state.selectedOptionIndex ?: return

        val newCorrectCount = if (selected == question.correctOptionIndex) state.correctCount + 1 else state.correctCount
        val isLastQuestion = state.currentIndex == state.questions.lastIndex

        if (isLastQuestion) {
            finish(state.copy(correctCount = newCorrectCount))
        } else {
            _uiState.value = state.copy(
                currentIndex = state.currentIndex + 1,
                selectedOptionIndex = null,
                correctCount = newCorrectCount
            )
        }
    }

    private fun finish(state: QuizUiState) {
        if (finished) return
        finished = true
        _uiState.value = state

        val total = state.questions.size
        val xp = state.correctCount * 10
        val score = "${state.correctCount}/$total"
        viewModelScope.launch {
            val resultArgs = gameRepository.recordFinishedGame(
                attempt = NewGameAttempt(
                    fileId = fileId,
                    gameType = GameType.QUIZ,
                    fileTitle = state.subjectTitle,
                    scoreLabel = score,
                    scorePercent = if (total == 0) 0 else state.correctCount * 100 / total,
                    xpEarned = xp,
                    bestScoreLabel = score
                ),
                resultArgs = GameResultArgs(
                    fileId = fileId,
                    gameType = GameType.QUIZ,
                    title = "${GameType.QUIZ.displayTitle()} — ${state.subjectTitle}",
                    big = score,
                    caption = "corretas",
                    xp = xp,
                    streak = 0,
                    note = if (state.correctCount == total) {
                        "Você acertou todas as perguntas. Excelente!"
                    } else {
                        "Continue praticando para melhorar sua pontuação."
                    }
                )
            )
            _uiState.value = _uiState.value.copy(resultArgs = resultArgs)
        }
    }
}
