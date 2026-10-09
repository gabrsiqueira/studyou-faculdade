package br.com.studyou.ui.screens.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.UserProfile
import br.com.studyou.domain.repository.AuthRepository
import br.com.studyou.domain.repository.HistoryRepository
import br.com.studyou.domain.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class ProfileUiState(val profile: UserProfile? = null)

/** Holds the state of the "Perfil" screen. */
class ProfileViewModel(
    userRepository: UserRepository,
    historyRepository: HistoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = userRepository.observeProfile()
        .map { ProfileUiState(profile = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    init {
        // Stats come from the user's attempts, so make sure they're loaded from the backend.
        viewModelScope.launch { historyRepository.refresh() }
    }

    /** Ends the session; the navigation host reacts to it and returns to the login screen. */
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
