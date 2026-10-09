package br.com.studyou.ui.screens.files

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import br.com.studyou.domain.model.FileStatus
import br.com.studyou.ui.theme.AprendixColors

data class FileStatusStyle(val label: String, val containerColor: Color, val contentColor: Color)

@Composable
fun fileStatusStyle(status: FileStatus): FileStatusStyle = when (status) {
    FileStatus.COMPLETED -> FileStatusStyle(
        "Concluído",
        AprendixColors.StatusSuccessContainer,
        AprendixColors.StatusSuccess
    )

    FileStatus.GENERATING_GAMES -> FileStatusStyle(
        "Processado",
        AprendixColors.StatusInfoContainer,
        AprendixColors.StatusInfo
    )

    FileStatus.ANALYZING -> FileStatusStyle(
        "Processando",
        AprendixColors.StatusWarningContainer,
        AprendixColors.StatusWarning
    )

    FileStatus.FAILED -> FileStatusStyle(
        "Erro",
        AprendixColors.StatusDangerContainer,
        AprendixColors.StatusDanger
    )
}

/** Short subject label derived from the file name, e.g. "Sistema Solar – Resumo.pdf" -> "Sistema Solar". */
fun subjectTitleOf(fileName: String): String =
    fileName.substringBeforeLast('.').ifBlank { fileName }
        .substringBefore(" – ").substringBefore(" - ").trim()
