package br.com.studyou.data.repository

import br.com.studyou.domain.model.GameHistoryGroup
import br.com.studyou.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Not yet wired to a backend/database, so the history starts empty — no placeholder attempts. */
class HistoryRepositoryImpl : HistoryRepository {

    private val historyFlow = MutableStateFlow<List<GameHistoryGroup>>(emptyList())

    override fun observeHistory(): Flow<List<GameHistoryGroup>> {
        // TODO: replace with a real API/DB call that streams the user's past game attempts.
        return historyFlow.asStateFlow()
    }
}
