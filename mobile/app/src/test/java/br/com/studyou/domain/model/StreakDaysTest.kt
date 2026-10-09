package br.com.studyou.domain.model

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakDaysTest {

    private val now: Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 18, 15, 0, 0)
    }.timeInMillis

    private fun daysAgo(days: Int, hour: Int = 10): Long = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -days)
        set(Calendar.HOUR_OF_DAY, hour)
    }.timeInMillis

    @Test
    fun noAttempts_isZero() {
        assertEquals(0, streakDays(emptyList(), now))
    }

    @Test
    fun countsConsecutiveDaysEndingToday_ignoringRepeatsOnTheSameDay() {
        val attempts = listOf(daysAgo(0), daysAgo(0, hour = 8), daysAgo(1), daysAgo(2), daysAgo(4))
        assertEquals(3, streakDays(attempts, now))
    }

    @Test
    fun aStreakEndingYesterdayStillCounts_untilAFullDayIsMissed() {
        assertEquals(2, streakDays(listOf(daysAgo(1), daysAgo(2)), now))
        assertEquals(0, streakDays(listOf(daysAgo(2), daysAgo(3)), now))
    }

    @Test
    fun firstGameEver_isAOneDayStreak() {
        assertEquals(1, streakDays(listOf(daysAgo(0)), now))
    }

    @Test
    fun playedAtLabels() {
        assertEquals("hoje", playedAtLabel(daysAgo(0), now))
        assertEquals("ontem", playedAtLabel(daysAgo(1), now))
    }
}
