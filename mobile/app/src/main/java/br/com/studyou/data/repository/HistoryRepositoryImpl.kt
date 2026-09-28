package br.com.studyou.data.repository

import br.com.studyou.domain.model.GameHistoryGroup
import br.com.studyou.domain.model.displayTitle
import br.com.studyou.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Groups the attempts recorded in [gameAttemptStore] by game type for the history screen. */
class HistoryRepositoryImpl(private val gameAttemptStore: GameAttemptStore) : HistoryRepository {

    override fun observeHistory(): Flow<List<GameHistoryGroup>> =
        gameAttemptStore.observe().map { attempts ->
            attempts
                .groupBy { it.gameType }
                .map { (type, groupAttempts) ->
                    GameHistoryGroup(
                        gameType = type,
                        title = type.displayTitle(),
                        subtitle = "${groupAttempts.size} tentativa(s)",
                        attempts = groupAttempts
                    )
                }
        }
}
