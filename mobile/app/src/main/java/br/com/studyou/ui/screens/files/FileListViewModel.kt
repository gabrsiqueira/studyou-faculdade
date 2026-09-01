package br.com.studyou.ui.screens.files

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.model.StudyFile
import br.com.studyou.domain.repository.FileRepository
import br.com.studyou.domain.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
data class FileListUiState(
    val userName: String = "",
    val files: List<StudyFile> = emptyList()
)

/** Holds the state of the "Meus arquivos" home screen. */
class FileListViewModel(
    fileRepository: FileRepository,
    userRepository: UserRepository
) : ViewModel() {

    val uiState: StateFlow<FileListUiState> = combine(
        fileRepository.observeFiles(),
        userRepository.observeProfile()
    ) { files, profile ->
        FileListUiState(userName = profile.name, files = files)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FileListUiState())
}
