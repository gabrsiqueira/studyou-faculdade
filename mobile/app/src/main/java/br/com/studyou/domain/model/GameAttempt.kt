package br.com.studyou.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** The outcome of a single play-through of a game, as persisted for the signed-in user. */
data class GameAttempt(
    val id: String,
    val fileId: String?,
    val gameType: GameType,
    val fileTitle: String,
    val playedAtMillis: Long,
    val scoreLabel: String,
    val scorePercent: Int,
    val xpEarned: Int,
    val bestScoreLabel: String
) {
    val playedAtLabel: String get() = playedAtLabel(playedAtMillis)
}

/** A finished game about to be recorded; the backend assigns its id and timestamp. */
data class NewGameAttempt(
    val fileId: String,
    val gameType: GameType,
    val fileTitle: String,
    val scoreLabel: String,
    val scorePercent: Int,
    val xpEarned: Int,
    val bestScoreLabel: String
)

/** A group of past attempts of the same [gameType], for the history screen. */
data class GameHistoryGroup(
    val gameType: GameType,
    val title: String,
    val subtitle: String,
    val attempts: List<GameAttempt>
)

private fun dayKey(calendar: Calendar): Int =
    calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)

private fun calendarAt(millis: Long): Calendar = Calendar.getInstance().apply { timeInMillis = millis }

/** "hoje", "ontem" or a short date such as "12 out", in the device's time zone. */
fun playedAtLabel(playedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val played = calendarAt(playedAtMillis)
    val today = calendarAt(nowMillis)
    val yesterday = calendarAt(nowMillis).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when (dayKey(played)) {
        dayKey(today) -> "hoje"
        dayKey(yesterday) -> "ontem"
        else -> SimpleDateFormat("dd MMM", Locale("pt", "BR")).format(played.time)
    }
}

/**
 * Number of consecutive days, ending today (or yesterday, if nothing was played yet today),
 * on which at least one game was played. 0 when the user hasn't played today or yesterday.
 */
fun streakDays(playedAtMillis: List<Long>, nowMillis: Long = System.currentTimeMillis()): Int {
    val playedDays = playedAtMillis.map { dayKey(calendarAt(it)) }.toSet()
    val cursor = calendarAt(nowMillis)
    if (dayKey(cursor) !in playedDays) cursor.add(Calendar.DAY_OF_YEAR, -1)

    var streak = 0
    while (dayKey(cursor) in playedDays) {
        streak++
        cursor.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}
