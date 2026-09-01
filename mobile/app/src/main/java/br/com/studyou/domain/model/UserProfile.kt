package br.com.studyou.domain.model

/** The signed-in user and their aggregated study stats. */
data class UserProfile(
    val name: String,
    val email: String,
    val completedGames: Int,
    val averageScorePercent: Int,
    val streakDays: Int
)
