package br.com.studyou.domain.model

/** Processing stage of a study file uploaded by the user. */
enum class FileStatus {
    ANALYZING,
    GENERATING_GAMES,
    COMPLETED,
    FAILED
}

/**
 * A study material uploaded by the user (PDF, slides, image, etc.) from which
 * games are generated.
 */
data class StudyFile(
    val id: String,
    val name: String,
    val status: FileStatus,
    val statusDetail: String
)
