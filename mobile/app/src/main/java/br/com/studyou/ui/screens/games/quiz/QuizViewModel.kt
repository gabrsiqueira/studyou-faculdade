package br.com.studyou.ui.screens.games.quiz

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.GameAttempt
import br.com.studyou.domain.model.GameType
import br.com.studyou.domain.model.QuizQuestion
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
        val total = state.questions.size
        val xp = state.correctCount * 10
        viewModelScope.launch {
            gameRepository.saveAttempt(
                GameAttempt(
                    id = "quiz-$fileId-${System.currentTimeMillis()}",
                    gameType = GameType.QUIZ,
                    fileTitle = state.subjectTitle,
                    playedAtLabel = "hoje",
                    scoreLabel = "${state.correctCount}/$total",
                    xpEarned = xp,
                    streakDays = 5,
                    bestScoreLabel = "${state.correctCount}/$total"
                )
            )
        }
        val note = if (state.correctCount == total) {
            "Você acertou todas as perguntas. Excelente!"
        } else {
            "Continue praticando para melhorar sua pontuação."
        }
        _uiState.value = state.copy(
            resultArgs = GameResultArgs(
                fileId = fileId,
                gameType = GameType.QUIZ,
                title = "Quiz — ${state.subjectTitle}",
                big = "${state.correctCount}/$total",
                caption = "corretas",
                xp = xp,
                streak = 5,
                note = note
            )
        )
    }
}
