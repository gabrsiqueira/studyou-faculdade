package br.com.studyou.domain.repository

import br.com.studyou.domain.model.CrosswordPuzzle
import br.com.studyou.domain.model.Flashcard
import br.com.studyou.domain.model.GameAttempt
import br.com.studyou.domain.model.GameSummary
import br.com.studyou.domain.model.GuessWordPuzzle
import br.com.studyou.domain.model.QuizQuestion
import br.com.studyou.domain.model.WordFormingPuzzle

/** Provides the games generated for a study file and records the results of playing them. */
interface GameRepository {
    suspend fun getGames(fileId: String): List<GameSummary>

    suspend fun getQuiz(fileId: String): List<QuizQuestion>

    suspend fun getFlashcards(fileId: String): List<Flashcard>

    /** Returns null when no puzzle has been generated yet for this file. */
    suspend fun getGuessWordPuzzle(fileId: String): GuessWordPuzzle?

    /** Returns null when no puzzle has been generated yet for this file. */
    suspend fun getCrosswordPuzzle(fileId: String): CrosswordPuzzle?

    /** Returns null when no puzzle has been generated yet for this file. */
    suspend fun getWordFormingPuzzle(fileId: String): WordFormingPuzzle?

    suspend fun saveAttempt(attempt: GameAttempt)
}
