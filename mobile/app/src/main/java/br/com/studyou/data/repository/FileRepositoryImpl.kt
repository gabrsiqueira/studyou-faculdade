package br.com.studyou.data.repository

import br.com.studyou.data.remote.FileAnalysisApiClient
import br.com.studyou.data.remote.FileAnalysisResult
import br.com.studyou.domain.model.FileStatus
import br.com.studyou.domain.model.StudyFile
import br.com.studyou.domain.notification.UploadResultNotifier
import br.com.studyou.domain.repository.FileRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Keeps the uploaded files in memory for the lifetime of the process. The list starts empty
 * and only grows as the user sends real files, whose content is actually posted to the
 * backend for analysis — nothing here is sample/placeholder data.
 *
 * Analysis runs on [processingScope], independent of any screen's lifecycle, so it keeps
 * going — and still notifies the user — even if they navigate away right after sending the file.
 */
class FileRepositoryImpl(
    private val apiClient: FileAnalysisApiClient,
    private val notifier: UploadResultNotifier,
    private val generatedGamesStore: GeneratedGamesStore
) : FileRepository {

    private val filesFlow = MutableStateFlow<List<StudyFile>>(emptyList())
    private val processingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeFiles(): Flow<List<StudyFile>> = filesFlow.asStateFlow()

    override suspend fun uploadFile(fileName: String, mimeType: String, content: ByteArray): Result<Unit> {
        val fileId = UUID.randomUUID().toString()
        filesFlow.update { current ->
            current + StudyFile(
                id = fileId,
                name = fileName,
                status = FileStatus.ANALYZING,
                statusDetail = "Analisando conteúdo..."
            )
        }

        processingScope.launch {
            when (val result = apiClient.analyzeFile(fileName, mimeType, content)) {
                is FileAnalysisResult.Success -> {
                    generatedGamesStore.put(fileId, result.games)
                    updateFile(fileId) { it.copy(status = FileStatus.COMPLETED, statusDetail = "Pronto") }
                    notifier.notifyAnalysisCompleted(fileName)
                }

                is FileAnalysisResult.Failure -> {
                    updateFile(fileId) { it.copy(status = FileStatus.FAILED, statusDetail = result.message) }
                    notifier.notifyAnalysisFailed(fileName, result.message)
                }
            }
        }

        return Result.success(Unit)
    }

    private fun updateFile(fileId: String, transform: (StudyFile) -> StudyFile) {
        filesFlow.update { current -> current.map { if (it.id == fileId) transform(it) else it } }
    }
}
