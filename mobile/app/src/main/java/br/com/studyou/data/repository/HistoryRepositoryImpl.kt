package br.com.studyou.data.repository

import br.com.studyou.data.remote.ApiResult
import br.com.studyou.data.remote.StudYouApiClient
import br.com.studyou.domain.model.GameHistoryGroup
import br.com.studyou.domain.model.displayTitle
import br.com.studyou.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The signed-in user's attempts, loaded from the backend and grouped by game type. */
class HistoryRepositoryImpl(
    private val api: StudYouApiClient,
    private val gameAttemptStore: GameAttemptStore
) : HistoryRepository {

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

    override suspend fun refresh(): Result<Unit> = when (val result = api.listAttempts()) {
        is ApiResult.Success -> {
            gameAttemptStore.replaceAll(result.data)
            Result.success(Unit)
        }
        is ApiResult.Failure -> Result.failure(IllegalStateException(result.message))
    }
}
