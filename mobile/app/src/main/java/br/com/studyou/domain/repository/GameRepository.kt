package br.com.studyou.domain.repository

import br.com.studyou.domain.model.CrosswordPuzzle
import br.com.studyou.domain.model.Flashcard
import br.com.studyou.domain.model.GameSummary
import br.com.studyou.domain.model.GuessWordPuzzle
import br.com.studyou.domain.model.NewGameAttempt
import br.com.studyou.domain.model.QuizQuestion

/** Provides the games generated for a study file and records the results of playing them. */
interface GameRepository {
    suspend fun getGames(fileId: String): List<GameSummary>

    suspend fun getQuiz(fileId: String): List<QuizQuestion>

    suspend fun getFlashcards(fileId: String): List<Flashcard>

    /** Returns null when no puzzle has been generated yet for this file. */
    suspend fun getGuessWordPuzzle(fileId: String): GuessWordPuzzle?

    /** Returns null when no puzzle has been generated yet for this file. */
    suspend fun getCrosswordPuzzle(fileId: String): CrosswordPuzzle?

    /** Records a finished game for the signed-in user so it shows up in history and profile. */
    suspend fun saveAttempt(attempt: NewGameAttempt): Result<Unit>

    /** Consecutive days (ending today) on which the signed-in user played at least one game. */
    suspend fun currentStreakDays(): Int

    /** Sum of the XP earned in every game the signed-in user has finished. */
    suspend fun totalXp(): Int
}
