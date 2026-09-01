package br.com.studyou.ui.screens.upload

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyou.domain.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024

@Immutable
data class SelectedFile(val name: String, val sizeBytes: Long, val mimeType: String, val content: ByteArray)

@Immutable
data class FileUploadUiState(
    val selectedFile: SelectedFile? = null,
    val isUploading: Boolean = false,
    val errorMessage: String? = null
)

/** Holds the state of the "Enviar arquivo" screen. */
class FileUploadViewModel(private val fileRepository: FileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FileUploadUiState())
    val uiState: StateFlow<FileUploadUiState> = _uiState.asStateFlow()

    fun onFileSelected(name: String, sizeBytes: Long, mimeType: String, content: ByteArray) {
        _uiState.value = if (sizeBytes > MAX_FILE_SIZE_BYTES) {
            _uiState.value.copy(errorMessage = "O arquivo excede o limite de 20 MB.")
        } else {
            _uiState.value.copy(
                selectedFile = SelectedFile(name, sizeBytes, mimeType, content),
                errorMessage = null
            )
        }
    }

    fun onRemoveSelectedFile() {
        _uiState.value = _uiState.value.copy(selectedFile = null)
    }

    fun upload(onSuccess: () -> Unit) {
        val file = _uiState.value.selectedFile ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, errorMessage = null)
            fileRepository.uploadFile(file.name, file.mimeType, file.content)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isUploading = false)
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        errorMessage = error.message ?: "Não foi possível enviar o arquivo."
                    )
                }
        }
    }
}
