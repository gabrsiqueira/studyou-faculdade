package br.com.studyou.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import br.com.studyou.domain.model.GameType
import br.com.studyou.ui.screens.result.GameResultArgs
import br.com.studyou.domain.model.CrosswordDirection
import br.com.studyou.domain.model.CrosswordEntry
import br.com.studyou.ui.screens.games.crossword.CrosswordScreen
import br.com.studyou.ui.screens.games.crossword.CrosswordUiState
import br.com.studyou.ui.screens.games.guessword.GuessWordScreen
import br.com.studyou.ui.screens.games.guessword.GuessWordUiState
import br.com.studyou.ui.screens.games.guessword.evaluateGuess
import br.com.studyou.ui.theme.StudyouTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Renders the letter-based game screens with realistic data to catch layout crashes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameScreensRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun crosswordScreen_rendersGridAndLetterTiles() {
        composeRule.setContent {
            StudyouTheme {
                CrosswordScreen(
                    uiState = CrosswordUiState(
                        subjectTitle = "Sistema Solar",
                        size = 5,
                        letters = "TRAMER".toList(),
                        entries = listOf(
                            CrosswordEntry(CrosswordDirection.DOWN, "TERRA", 0, 2, clue = "Planeta onde vivemos"),
                            CrosswordEntry(CrosswordDirection.ACROSS, "MARTE", 2, 0, clue = "Planeta vermelho")
                        ),
                        solvedEntries = setOf(CrosswordEntry(CrosswordDirection.DOWN, "TERRA", 0, 2, clue = "Planeta onde vivemos")),
                        isLoading = false
                    ),
                    onClose = {},
                    onLetterTap = {},
                    onClear = {},
                    onShuffle = {},
                    onConfirm = {}
                )
            }
        }

        composeRule.onNodeWithText("M").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Planeta vermelho (5 letras)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("TERRA — Planeta onde vivemos").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun guessWordScreen_solved_keepsBoardAndShowsFinishButton() {
        var finishClicks = 0
        composeRule.setContent {
            StudyouTheme {
                GuessWordScreen(
                    uiState = GuessWordUiState(
                        subjectTitle = "ENEM",
                        answerLength = 4,
                        maxAttempts = 6,
                        guesses = listOf("MESA", "ENEM").map { it.toList().zip(evaluateGuess(it, "ENEM")) },
                        isSolved = true,
                        isLoading = false,
                        pendingResultArgs = GameResultArgs(
                            fileId = "file-1",
                            gameType = GameType.GUESS_WORD,
                            title = "Termo",
                            big = "2 tentativas",
                            caption = "para acertar",
                            xp = 75,
                            streak = 0,
                            note = ""
                        )
                    ),
                    onClose = {},
                    onLetterClick = {},
                    onBackspaceClick = {},
                    onSubmitClick = {},
                    onFinishClick = { finishClicks++ }
                )
            }
        }

        composeRule.onNodeWithText("Você acertou em 2 de 6 tentativas!").assertIsDisplayed()
        composeRule.onNodeWithText("Finalizar").assertIsDisplayed().performClick()
        assertEquals(1, finishClicks)
    }

    @Test
    fun crosswordScreen_complete_keepsGridAndShowsFinishButton() {
        val entries = listOf(
            CrosswordEntry(CrosswordDirection.DOWN, "TERRA", 0, 2),
            CrosswordEntry(CrosswordDirection.ACROSS, "MARTE", 2, 0)
        )
        var finishClicks = 0
        composeRule.setContent {
            StudyouTheme {
                CrosswordScreen(
                    uiState = CrosswordUiState(
                        subjectTitle = "Sistema Solar",
                        size = 5,
                        letters = "TRAMER".toList(),
                        entries = entries,
                        solvedEntries = entries.toSet(),
                        isComplete = true,
                        isLoading = false,
                        pendingResultArgs = GameResultArgs(
                            fileId = "file-1",
                            gameType = GameType.CROSSWORD,
                            title = "Palavras Cruzadas",
                            big = "1:05",
                            caption = "para completar",
                            xp = 60,
                            streak = 0,
                            note = ""
                        )
                    ),
                    onClose = {},
                    onLetterTap = {},
                    onClear = {},
                    onShuffle = {},
                    onConfirm = {},
                    onFinishClick = { finishClicks++ }
                )
            }
        }

        composeRule.onNodeWithText("Confirmar palavra").assertDoesNotExist()
        composeRule.onNodeWithText("Finalizar").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, finishClicks)
    }
}
