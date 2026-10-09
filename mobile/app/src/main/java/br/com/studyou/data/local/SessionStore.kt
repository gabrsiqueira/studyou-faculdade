package br.com.studyou.data.local

import android.content.Context
import br.com.studyou.domain.model.AuthUser
import br.com.studyou.domain.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "studyou_session"
private const val KEY_TOKEN = "token"
private const val KEY_USER_ID = "user_id"
private const val KEY_USER_NAME = "user_name"
private const val KEY_USER_EMAIL = "user_email"

/**
 * Persists the signed-in [Session] across app restarts, so the user stays logged in until
 * they log out or the backend rejects the token.
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val sessionFlow = MutableStateFlow(read())

    val session: StateFlow<Session?> = sessionFlow.asStateFlow()

    fun save(session: Session) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_USER_NAME, session.user.name)
            .putString(KEY_USER_EMAIL, session.user.email)
            .apply()
        sessionFlow.value = session
    }

    fun clear() {
        prefs.edit().clear().apply()
        sessionFlow.value = null
    }

    private fun read(): Session? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return Session(
            token = token,
            user = AuthUser(
                id = prefs.getString(KEY_USER_ID, null).orEmpty(),
                name = prefs.getString(KEY_USER_NAME, null).orEmpty(),
                email = prefs.getString(KEY_USER_EMAIL, null).orEmpty()
            )
        )
    }
}
