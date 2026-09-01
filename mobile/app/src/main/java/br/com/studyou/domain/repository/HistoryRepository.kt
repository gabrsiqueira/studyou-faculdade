package br.com.studyou.domain.repository

import br.com.studyou.domain.model.GameHistoryGroup
import kotlinx.coroutines.flow.Flow

/** Provides the history of games already played by the user. */
interface HistoryRepository {
    fun observeHistory(): Flow<List<GameHistoryGroup>>
}
