package br.com.studyou.data.repository

import br.com.studyou.domain.model.UserProfile
import br.com.studyou.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private val EMPTY_PROFILE = UserProfile(
    name = "",
    email = "",
    completedGames = 0,
    averageScorePercent = 0,
    streakDays = 0
)

/** Not yet wired to a backend/identity provider, so the profile starts empty — no placeholder user. */
class UserRepositoryImpl : UserRepository {

    private val profileFlow = MutableStateFlow(EMPTY_PROFILE)

    override fun observeProfile(): Flow<UserProfile> {
        // TODO: replace with a real API/DB call that streams the signed-in user's profile and stats.
        return profileFlow.asStateFlow()
    }
}
