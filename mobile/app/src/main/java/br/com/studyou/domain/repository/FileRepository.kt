package br.com.studyou.domain.repository

import br.com.studyou.domain.model.StudyFile
import kotlinx.coroutines.flow.Flow

/** Manages the study files uploaded by the user. */
interface FileRepository {
    /** Streams the current list of files, including live status updates while they are processed. */
    fun observeFiles(): Flow<List<StudyFile>>

    /**
     * Uploads [content] (the raw bytes of [fileName]) for analysis. Appends the file to
     * [observeFiles] immediately in an "analyzing" state, then updates it to completed or
     * failed once the backend responds.
     */
    suspend fun uploadFile(fileName: String, mimeType: String, content: ByteArray): Result<Unit>
}
