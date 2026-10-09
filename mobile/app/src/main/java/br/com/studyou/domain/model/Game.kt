package br.com.studyou.domain.model

/** The kind of game that can be generated from a study file. */
enum class GameType {
    QUIZ,
    FLASHCARDS,
    GUESS_WORD,
    CROSSWORD
}

/** Short display name shown across the games list, result and history screens. */
fun GameType.displayTitle(): String = when (this) {
    GameType.QUIZ -> "Quiz"
    GameType.FLASHCARDS -> "Flashcards"
    GameType.GUESS_WORD -> "Termo"
    GameType.CROSSWORD -> "Palavras Cruzadas"
}

/** A game available for a given study file, as shown in the games list screen. */
data class GameSummary(
    val type: GameType,
    val title: String,
    val description: String,
    val resultLabel: String,
    val played: Boolean
)
