package br.com.studyou.data.repository

import br.com.studyou.data.local.SessionStore
import br.com.studyou.data.remote.ApiResult
import br.com.studyou.data.remote.StudYouApiClient
import br.com.studyou.domain.model.Session
import br.com.studyou.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Registers/logs in against the backend and keeps the resulting session in [sessionStore]. */
class AuthRepositoryImpl(
    private val api: StudYouApiClient,
    private val sessionStore: SessionStore
) : AuthRepository {

    override fun observeIsLoggedIn(): Flow<Boolean> =
        sessionStore.session.map { it != null }.distinctUntilChanged()

    override fun isLoggedIn(): Boolean = sessionStore.session.value != null

    override suspend fun login(email: String, password: String): Result<Unit> =
        startSession(api.login(email.trim(), password))

    override suspend fun register(name: String, email: String, password: String): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Informe seu nome."))
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres."))
        }
        return startSession(api.register(name.trim(), email.trim(), password))
    }

    override suspend fun logout() {
        sessionStore.clear()
    }

    private fun startSession(result: ApiResult<Session>): Result<Unit> = when (result) {
        is ApiResult.Success -> {
            sessionStore.save(result.data)
            Result.success(Unit)
        }
        is ApiResult.Failure -> Result.failure(IllegalStateException(result.message))
    }
}
