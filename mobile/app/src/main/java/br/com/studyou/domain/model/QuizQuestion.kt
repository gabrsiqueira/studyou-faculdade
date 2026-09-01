package br.com.studyou.domain.model

/** A single multiple-choice question of a Quiz game. */
data class QuizQuestion(
    val id: String,
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int
)
