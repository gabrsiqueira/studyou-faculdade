package br.com.studyou.di

import android.content.Context
import br.com.studyou.BuildConfig
import br.com.studyou.data.local.SessionStore
import br.com.studyou.data.notification.AndroidUploadResultNotifier
import br.com.studyou.data.remote.StudYouApiClient
import br.com.studyou.data.repository.AuthRepositoryImpl
import br.com.studyou.data.repository.FileRepositoryImpl
import br.com.studyou.data.repository.GameAttemptStore
import br.com.studyou.data.repository.GameRepositoryImpl
import br.com.studyou.data.repository.GeneratedGamesStore
import br.com.studyou.data.repository.HistoryRepositoryImpl
import br.com.studyou.data.repository.UserRepositoryImpl
import br.com.studyou.domain.repository.AuthRepository
import br.com.studyou.domain.repository.FileRepository
import br.com.studyou.domain.repository.GameRepository
import br.com.studyou.domain.repository.HistoryRepository
import br.com.studyou.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Minimal manual dependency container. ViewModels depend only on the repository interfaces
 * (Dependency Inversion), so swapping an implementation only means changing the wiring here.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val sessionStore = SessionStore(appContext)

    private val api = StudYouApiClient(
        baseUrl = BuildConfig.BACKEND_BASE_URL,
        tokenProvider = { sessionStore.session.value?.token },
        // The backend rejected the token (expired/revoked): end the session, which sends
        // the user back to the login screen.
        onUnauthorized = { sessionStore.clear() }
    )

    // Per-user caches of what the backend has: generated games per file, and game attempts.
    private val generatedGamesStore = GeneratedGamesStore()
    private val gameAttemptStore = GameAttemptStore()
    private val fileRepositoryImpl = FileRepositoryImpl(
        api = api,
        notifier = AndroidUploadResultNotifier(appContext),
        generatedGamesStore = generatedGamesStore
    )

    val authRepository: AuthRepository = AuthRepositoryImpl(api, sessionStore)
    val fileRepository: FileRepository = fileRepositoryImpl
    val gameRepository: GameRepository = GameRepositoryImpl(api, generatedGamesStore, gameAttemptStore)
    val historyRepository: HistoryRepository = HistoryRepositoryImpl(api, gameAttemptStore)
    val userRepository: UserRepository = UserRepositoryImpl(sessionStore, gameAttemptStore)

    init {
        // Whenever the signed-in user changes (logout, expired session or another account
        // logging in), drop the previous user's cached data so it never leaks across accounts.
        appScope.launch {
            sessionStore.session
                .map { it?.user?.id }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    fileRepositoryImpl.clear()
                    generatedGamesStore.clear()
                    gameAttemptStore.clear()
                }
        }
    }

    companion object {
        @Volatile private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
    }
}
