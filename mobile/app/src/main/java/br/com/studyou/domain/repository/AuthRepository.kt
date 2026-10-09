package br.com.studyou.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Handles authentication of the user against the backend. A successful [login]/[register]
 * keeps the user signed in across app restarts until [logout] or the session expires.
 */
interface AuthRepository {
    /** Emits whether a user is currently signed in; flips to false on logout or expiry. */
    fun observeIsLoggedIn(): Flow<Boolean>

    fun isLoggedIn(): Boolean

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun register(name: String, email: String, password: String): Result<Unit>

    suspend fun logout()
}
