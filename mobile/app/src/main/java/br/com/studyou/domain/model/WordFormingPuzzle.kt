package br.com.studyou.domain.model

/** Letters the player can combine to form any of the [validWords]. */
data class WordFormingPuzzle(
    val title: String,
    val letters: List<Char>,
    val validWords: Set<String>
)
