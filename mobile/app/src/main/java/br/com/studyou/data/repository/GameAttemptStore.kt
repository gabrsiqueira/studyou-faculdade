package br.com.studyou.data.repository

import br.com.studyou.domain.model.GameAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the game attempts played this session, in memory. Shared between
 * [GameRepositoryImpl] (which records an attempt when a game finishes) and
 * [HistoryRepositoryImpl] (which reads them for the "Meu histórico" screen).
 */
class GameAttemptStore {
    private val attemptsFlow = MutableStateFlow<List<GameAttempt>>(emptyList())

    fun observe(): StateFlow<List<GameAttempt>> = attemptsFlow.asStateFlow()

    fun add(attempt: GameAttempt) {
        attemptsFlow.update { current -> listOf(attempt) + current }
    }
}
