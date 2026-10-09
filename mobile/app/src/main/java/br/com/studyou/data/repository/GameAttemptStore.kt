package br.com.studyou.data.repository

import br.com.studyou.domain.model.GameAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory cache of the signed-in user's game attempts (most recent first), mirroring what
 * the backend has. Shared by [GameRepositoryImpl] (records new attempts), [HistoryRepositoryImpl]
 * and [UserRepositoryImpl] (profile stats). Cleared when the user logs out.
 */
class GameAttemptStore {
    private val attemptsFlow = MutableStateFlow<List<GameAttempt>>(emptyList())

    fun observe(): StateFlow<List<GameAttempt>> = attemptsFlow.asStateFlow()

    fun current(): List<GameAttempt> = attemptsFlow.value

    fun add(attempt: GameAttempt) {
        attemptsFlow.update { current -> listOf(attempt) + current.filterNot { it.id == attempt.id } }
    }

    fun replaceAll(attempts: List<GameAttempt>) {
        attemptsFlow.value = attempts.sortedByDescending { it.playedAtMillis }
    }

    fun clear() {
        attemptsFlow.value = emptyList()
    }
}
