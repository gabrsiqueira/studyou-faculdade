package br.com.studyou.data.repository

import br.com.studyou.domain.repository.AuthRepository

/** Stub implementation. Not yet wired to a backend/identity provider. */
class AuthRepositoryImpl : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        // TODO: call the authentication API with (email, password) and persist the session token.
        return Result.success(Unit)
    }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> {
        // TODO: call the API to create the user account and persist the returned session token.
        return Result.success(Unit)
    }

    override suspend fun logout() {
        // TODO: clear the persisted session token and notify the backend, if applicable.
    }
}
