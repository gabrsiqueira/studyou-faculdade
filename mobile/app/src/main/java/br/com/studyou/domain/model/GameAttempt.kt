package br.com.studyou.domain.model

/** The outcome of a single play-through of a game, used to build history and results. */
data class GameAttempt(
    val id: String,
    val gameType: GameType,
    val fileTitle: String,
    val playedAtLabel: String,
    val scoreLabel: String,
    val xpEarned: Int,
    val streakDays: Int,
    val bestScoreLabel: String
)

/** A group of past attempts of the same [gameType], for the history screen. */
data class GameHistoryGroup(
    val gameType: GameType,
    val title: String,
    val subtitle: String,
    val attempts: List<GameAttempt>
)
