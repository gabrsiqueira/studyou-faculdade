package br.com.studyou.ui

import android.os.Looper
import br.com.studyou.domain.model.CrosswordPuzzle
import br.com.studyou.domain.model.FileStatus
import br.com.studyou.domain.model.Flashcard
import br.com.studyou.domain.model.GameSummary
import br.com.studyou.domain.model.GameType
import br.com.studyou.domain.model.GuessWordPuzzle
import br.com.studyou.domain.model.NewGameAttempt
import br.com.studyou.domain.model.QuizQuestion
import br.com.studyou.domain.model.StudyFile
import br.com.studyou.domain.repository.FileRepository
import br.com.studyou.domain.repository.GameRepository
import br.com.studyou.ui.screens.games.flashcards.FlashcardsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FlashcardsViewModelTest {

    private class FakeFileRepository : FileRepository {
        override fun observeFiles(): Flow<List<StudyFile>> =
            flowOf(listOf(StudyFile("file-1", "Coracao.pdf", FileStatus.COMPLETED, "Pronto")))
        override suspend fun refresh() = Result.success(Unit)
        override suspend fun uploadFile(fileName: String, mimeType: String, content: ByteArray) = Result.success(Unit)
    }

    private class FakeGameRepository(private val saveFails: Boolean = false) : GameRepository {
        val saved = mutableListOf<NewGameAttempt>()
        override suspend fun getGames(fileId: String): List<GameSummary> = emptyList()
        override suspend fun getQuiz(fileId: String): List<QuizQuestion> = emptyList()
        override suspend fun getFlashcards(fileId: String) = listOf(
            Flashcard("1", "Quantas camaras?", "Quatro"),
            Flashcard("2", "O que impede o refluxo?", "As valvulas")
        )
        override suspend fun getGuessWordPuzzle(fileId: String): GuessWordPuzzle? = null
        override suspend fun getCrosswordPuzzle(fileId: String): CrosswordPuzzle? = null
        override suspend fun saveAttempt(attempt: NewGameAttempt): Result<Unit> {
            saved += attempt
            return if (saveFails) Result.failure(IllegalStateException("sem conexão")) else Result.success(Unit)
        }
        override suspend fun currentStreakDays() = 3
        override suspend fun totalXp() = saved.sumOf { it.xpEarned }
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun rememberingTheLastCard_finishesOnce_evenWhenTappedTwice() {
        val games = FakeGameRepository()
        val viewModel = FlashcardsViewModel("file-1", FakeFileRepository(), games)
        idle()

        viewModel.markCard(remembered = true)
        viewModel.markCard(remembered = true) // last card
        viewModel.markCard(remembered = true) // accidental double tap
        idle()

        assertEquals(1, games.saved.size)
        assertEquals(GameType.FLASHCARDS, games.saved.single().gameType)
        assertEquals("file-1", games.saved.single().fileId)
        assertEquals(100, games.saved.single().scorePercent)

        val result = viewModel.uiState.value.resultArgs
        assertNotNull(result)
        assertEquals("100%", result!!.big)
        assertEquals(3, result.streak)
    }

    @Test
    fun whenSavingFails_theResultIsStillShown_withANote() {
        val viewModel = FlashcardsViewModel("file-1", FakeFileRepository(), FakeGameRepository(saveFails = true))
        idle()

        viewModel.markCard(remembered = false)
        viewModel.markCard(remembered = true)
        idle()

        val result = viewModel.uiState.value.resultArgs!!
        assertEquals("50%", result.big)
        assertTrue(result.note.contains("Não foi possível salvar no histórico"))
    }
}
