package br.com.studyou.ui.screens.files

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.StudyFile
import br.com.studyou.domain.repository.FileRepository
import br.com.studyou.domain.repository.HistoryRepository
import br.com.studyou.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class FileListUiState(
    val userName: String = "",
    val files: List<StudyFile> = emptyList(),
    val errorMessage: String? = null
)

/** Holds the state of the "Meus arquivos" home screen. */
class FileListViewModel(
    private val fileRepository: FileRepository,
    userRepository: UserRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FileListUiState> = combine(
        fileRepository.observeFiles(),
        userRepository.observeProfile(),
        errorMessage
    ) { files, profile, error ->
        FileListUiState(userName = profile.name, files = files, errorMessage = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FileListUiState())

    init {
        refresh()
    }

    /** Loads the user's files (and their attempts, used for each game's best result) from the backend. */
    fun refresh() {
        viewModelScope.launch {
            errorMessage.value = fileRepository.refresh().exceptionOrNull()?.message
            historyRepository.refresh()
        }
    }
}
