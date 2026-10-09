package br.com.studyou.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelProgressTest {

    @Test
    fun startsAtLevelOneWithNoXp() {
        val progress = levelProgressFor(0)
        assertEquals(1, progress.level)
        assertEquals(0, progress.xpIntoLevel)
        assertEquals(100, progress.xpForNextLevel)
    }

    @Test
    fun eachLevelTakesLevelTimesHundredXp() {
        assertEquals(1, levelProgressFor(99).level)
        assertEquals(2, levelProgressFor(100).level)
        assertEquals(2, levelProgressFor(299).level)
        assertEquals(3, levelProgressFor(300).level)
        assertEquals(4, levelProgressFor(600).level)
    }

    @Test
    fun reportsProgressInsideTheCurrentLevel() {
        val progress = levelProgressFor(420)
        assertEquals(3, progress.level)
        assertEquals(120, progress.xpIntoLevel)
        assertEquals(300, progress.xpForNextLevel)
        assertEquals(180, progress.xpToNextLevel)
    }
}
