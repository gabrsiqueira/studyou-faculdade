package br.com.studyou.domain.model

/** The signed-in user, as returned by the backend on register/login. */
data class AuthUser(val id: String, val name: String, val email: String)

/** An authenticated session: the backend token sent on every request, plus who it belongs to. */
data class Session(val token: String, val user: AuthUser)
