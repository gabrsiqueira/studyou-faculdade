package br.com.studyou.domain.model

/** Wordle-like puzzle where the player guesses [answer] within [maxAttempts]. */
data class GuessWordPuzzle(
    val title: String,
    val answer: String,
    val maxAttempts: Int
)
