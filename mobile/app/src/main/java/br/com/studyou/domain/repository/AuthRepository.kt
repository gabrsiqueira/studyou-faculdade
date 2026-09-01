package br.com.studyou.domain.repository

/**
 * Handles authentication of the user. Implementations talk to the backend/identity provider.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun register(name: String, email: String, password: String): Result<Unit>

    suspend fun logout()
}
