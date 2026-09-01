package br.com.studyou.domain.model

enum class CrosswordDirection { ACROSS, DOWN }

/** One word placed on the crossword grid, starting at ([row], [col]). */
data class CrosswordEntry(
    val direction: CrosswordDirection,
    val answer: String,
    val row: Int,
    val col: Int
)

/**
 * A Word-of-Wonders-style "Cruzamento": the player forms words by tapping letters from
 * the shared [letters] pool. A formed word that matches one of the [entries] is placed
 * automatically on the [size] x [size] grid — there are no text clues. Words that use the
 * same letters but are not part of the grid count as [bonusWords].
 */
data class CrosswordPuzzle(
    val title: String,
    val size: Int,
    val letters: List<Char>,
    val entries: List<CrosswordEntry>,
    val bonusWords: Set<String> = emptySet()
)
