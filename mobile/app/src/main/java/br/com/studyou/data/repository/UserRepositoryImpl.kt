package br.com.studyou.data.repository

import br.com.studyou.data.local.SessionStore
import br.com.studyou.domain.model.UserProfile
import br.com.studyou.domain.model.streakDays
import br.com.studyou.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** The signed-in user (from their session) with stats computed from their recorded attempts. */
class UserRepositoryImpl(
    private val sessionStore: SessionStore,
    private val gameAttemptStore: GameAttemptStore
) : UserRepository {

    override fun observeProfile(): Flow<UserProfile> =
        combine(sessionStore.session, gameAttemptStore.observe()) { session, attempts ->
            UserProfile(
                name = session?.user?.name.orEmpty(),
                email = session?.user?.email.orEmpty(),
                completedGames = attempts.size,
                averageScorePercent = if (attempts.isEmpty()) 0 else attempts.sumOf { it.scorePercent } / attempts.size,
                streakDays = streakDays(attempts.map { it.playedAtMillis }),
                totalXp = attempts.sumOf { it.xpEarned }
            )
        }
}
