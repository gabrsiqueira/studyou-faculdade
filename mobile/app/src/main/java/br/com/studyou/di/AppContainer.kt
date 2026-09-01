package br.com.studyou.di

import android.content.Context
import br.com.studyou.data.notification.AndroidUploadResultNotifier
import br.com.studyou.data.remote.FileAnalysisApiClient
import br.com.studyou.data.repository.AuthRepositoryImpl
import br.com.studyou.data.repository.FileRepositoryImpl
import br.com.studyou.data.repository.GameRepositoryImpl
import br.com.studyou.data.repository.HistoryRepositoryImpl
import br.com.studyou.data.repository.UserRepositoryImpl
import br.com.studyou.domain.repository.AuthRepository
import br.com.studyou.domain.repository.FileRepository
import br.com.studyou.domain.repository.GameRepository
import br.com.studyou.domain.repository.HistoryRepository
import br.com.studyou.domain.repository.UserRepository

/**
 * Minimal manual dependency container. ViewModels depend only on the repository interfaces
 * (Dependency Inversion), so swapping any stub [data.repository] implementation for a real
 * networked one later only means changing the wiring here.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val authRepository: AuthRepository by lazy { AuthRepositoryImpl() }
    val fileRepository: FileRepository by lazy {
        FileRepositoryImpl(
            apiClient = FileAnalysisApiClient(),
            notifier = AndroidUploadResultNotifier(appContext)
        )
    }
    val gameRepository: GameRepository by lazy { GameRepositoryImpl() }
    val historyRepository: HistoryRepository by lazy { HistoryRepositoryImpl() }
    val userRepository: UserRepository by lazy { UserRepositoryImpl() }

    companion object {
        @Volatile private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
    }
}
