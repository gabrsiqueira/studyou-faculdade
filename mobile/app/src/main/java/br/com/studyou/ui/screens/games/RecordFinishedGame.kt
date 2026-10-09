package br.com.studyou.ui.screens.games

import br.com.studyou.domain.model.NewGameAttempt
import br.com.studyou.domain.model.levelProgressFor
import br.com.studyou.domain.repository.GameRepository
import br.com.studyou.ui.screens.result.GameResultArgs

/**
 * Records a finished game for the signed-in user and returns [resultArgs] completed with
 * their real day streak and level (flagging when this game's XP made them level up). If the attempt can't be saved (e.g. no connection), the result is
 * still shown, with a note saying it didn't make it into the history.
 */
suspend fun GameRepository.recordFinishedGame(attempt: NewGameAttempt, resultArgs: GameResultArgs): GameResultArgs {
    val levelBefore = levelProgressFor(totalXp()).level
    val saveError = saveAttempt(attempt).exceptionOrNull()?.message
    // If saving failed the XP isn't in the history yet, but the player still earned it.
    val levelAfter = levelProgressFor(totalXp() + if (saveError == null) 0 else attempt.xpEarned).level
    return resultArgs.copy(
        streak = currentStreakDays(),
        level = levelAfter,
        leveledUp = levelAfter > levelBefore,
        note = if (saveError == null) {
            resultArgs.note
        } else {
            "${resultArgs.note}\n\nNão foi possível salvar no histórico: $saveError"
        }
    )
}
