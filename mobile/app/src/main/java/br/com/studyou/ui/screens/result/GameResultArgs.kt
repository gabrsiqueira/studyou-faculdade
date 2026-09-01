package br.com.studyou.ui.screens.result

import br.com.studyou.domain.model.GameType

/** Everything the result screen needs to render, computed by a game's ViewModel once it finishes. */
data class GameResultArgs(
    val fileId: String,
    val gameType: GameType,
    val title: String,
    val big: String,
    val caption: String,
    val xp: Int,
    val streak: Int,
    val note: String
)
