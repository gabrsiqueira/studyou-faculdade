package br.com.studyou.domain.model

enum class CrosswordDirection { ACROSS, DOWN }

/** One word placed on the crossword grid, starting at ([row], [col]). */
data class CrosswordEntry(
    val direction: CrosswordDirection,
    val answer: String,
    val row: Int,
    val col: Int,
    /** Hint shown to the player to help them find [answer]; empty for puzzles generated before hints. */
    val clue: String = ""
)

/**
 * A Word-of-Wonders-style "Palavras Cruzadas": the player forms words by tapping letters from
 * the shared [letters] pool, guided by each entry's clue. A formed word that matches one of
 * the [entries] is placed automatically on the [size] x [size] grid. Words that use the same
 * letters but are not part of the grid count as [bonusWords].
 */
data class CrosswordPuzzle(
    val title: String,
    val size: Int,
    val letters: List<Char>,
    val entries: List<CrosswordEntry>,
    val bonusWords: Set<String> = emptySet()
)
