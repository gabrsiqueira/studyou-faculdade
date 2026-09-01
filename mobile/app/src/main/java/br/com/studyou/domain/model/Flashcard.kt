package br.com.studyou.domain.model

/** A single question/answer flashcard. */
data class Flashcard(
    val id: String,
    val question: String,
    val answer: String
)
