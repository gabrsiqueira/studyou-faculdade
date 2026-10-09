package br.com.studyou.data.repository

import br.com.studyou.data.remote.AnalyzedFile
import br.com.studyou.data.remote.ApiResult
import br.com.studyou.data.remote.StudYouApiClient
import br.com.studyou.domain.model.FileStatus
import br.com.studyou.domain.model.StudyFile
import br.com.studyou.domain.notification.UploadResultNotifier
import br.com.studyou.domain.repository.FileRepository
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The signed-in user's files: the ones already analyzed come from the backend ([refresh]),
 * and new uploads are shown immediately as "analyzing" until the backend answers.
 *
 * Analysis runs on [processingScope], independent of any screen's lifecycle, so it keeps
 * going — and still notifies the user — even if they navigate away right after sending the file.
 */
class FileRepositoryImpl(
    private val api: StudYouApiClient,
    private val notifier: UploadResultNotifier,
    private val generatedGamesStore: GeneratedGamesStore
) : FileRepository {

    private val filesFlow = MutableStateFlow<List<StudyFile>>(emptyList())
    private val processingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Bumped on [clear] (logout), so an upload still in flight from the previous user doesn't
    // land in the next user's list when it finishes.
    private val sessionGeneration = AtomicInteger(0)

    override fun observeFiles(): Flow<List<StudyFile>> = filesFlow.asStateFlow()

    override suspend fun refresh(): Result<Unit> {
        val generation = sessionGeneration.get()
        return when (val result = api.listFiles()) {
            is ApiResult.Success -> {
                if (generation == sessionGeneration.get()) {
                    result.data.forEach { generatedGamesStore.put(it.id, it.games) }
                    filesFlow.update { current ->
                        val stillProcessing = current.filter { it.status != FileStatus.COMPLETED }
                        stillProcessing + result.data.map { it.toStudyFile() }
                    }
                }
                Result.success(Unit)
            }
            is ApiResult.Failure -> Result.failure(IllegalStateException(result.message))
        }
    }

    override suspend fun uploadFile(fileName: String, mimeType: String, content: ByteArray): Result<Unit> {
        val generation = sessionGeneration.get()
        val localId = "upload-${UUID.randomUUID()}"
        filesFlow.update { current ->
            listOf(
                StudyFile(id = localId, name = fileName, status = FileStatus.ANALYZING, statusDetail = "Analisando conteúdo...")
            ) + current
        }

        processingScope.launch {
            val result = api.analyzeFile(fileName, mimeType, content)
            if (generation != sessionGeneration.get()) return@launch

            when (result) {
                is ApiResult.Success -> {
                    generatedGamesStore.put(result.data.id, result.data.games)
                    filesFlow.update { current ->
                        current.map { if (it.id == localId) result.data.toStudyFile() else it }
                    }
                    notifier.notifyAnalysisCompleted(fileName)
                }

                is ApiResult.Failure -> {
                    filesFlow.update { current ->
                        current.map {
                            if (it.id == localId) it.copy(status = FileStatus.FAILED, statusDetail = result.message) else it
                        }
                    }
                    notifier.notifyAnalysisFailed(fileName, result.message)
                }
            }
        }

        return Result.success(Unit)
    }

    /** Forgets the current user's files; called when they log out or the session expires. */
    fun clear() {
        sessionGeneration.incrementAndGet()
        filesFlow.value = emptyList()
    }

    private fun AnalyzedFile.toStudyFile() =
        StudyFile(id = id, name = fileName, status = FileStatus.COMPLETED, statusDetail = "Pronto")
}
