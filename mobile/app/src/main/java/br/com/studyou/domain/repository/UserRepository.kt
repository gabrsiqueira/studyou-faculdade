package br.com.studyou.domain.repository

import br.com.studyou.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Provides the signed-in user's profile and aggregated stats. */
interface UserRepository {
    fun observeProfile(): Flow<UserProfile>
}
