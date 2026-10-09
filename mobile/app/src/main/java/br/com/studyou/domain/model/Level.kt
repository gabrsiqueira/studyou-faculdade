package br.com.studyou.domain.model

/** XP needed to go from level N to level N + 1 is N × this value (100, 200, 300…). */
private const val XP_PER_LEVEL_STEP = 100

/** Where a total amount of XP puts the player: their level and how far into it they are. */
data class LevelProgress(
    val level: Int,
    val totalXp: Int,
    /** XP earned since reaching [level]. */
    val xpIntoLevel: Int,
    /** XP the current level takes in total, from reaching it to reaching the next one. */
    val xpForNextLevel: Int
) {
    val xpToNextLevel: Int get() = xpForNextLevel - xpIntoLevel
    val progressFraction: Float get() = if (xpForNextLevel == 0) 0f else xpIntoLevel.toFloat() / xpForNextLevel
}

/** Everyone starts at level 1; going from level N to N + 1 takes N × 100 XP. */
fun levelProgressFor(totalXp: Int): LevelProgress {
    var level = 1
    var remaining = totalXp.coerceAtLeast(0)
    while (remaining >= level * XP_PER_LEVEL_STEP) {
        remaining -= level * XP_PER_LEVEL_STEP
        level++
    }
    return LevelProgress(
        level = level,
        totalXp = totalXp.coerceAtLeast(0),
        xpIntoLevel = remaining,
        xpForNextLevel = level * XP_PER_LEVEL_STEP
    )
}
