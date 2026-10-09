package br.com.studyou.ui.screens.games.crossword

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.studyou.domain.model.CrosswordDirection
import br.com.studyou.domain.model.CrosswordEntry
import br.com.studyou.domain.model.GameType
import br.com.studyou.domain.model.displayTitle
import br.com.studyou.ui.components.CloseTopBar
import br.com.studyou.ui.components.CurrentWordBanner
import br.com.studyou.ui.components.GameUnavailableMessage
import br.com.studyou.ui.components.LabelChip
import br.com.studyou.ui.components.LetterTileGrid
import br.com.studyou.ui.components.PrimaryButton
import br.com.studyou.ui.components.SecondaryButton
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.screens.result.GameResultArgs
import br.com.studyou.ui.theme.AprendixColors
import br.com.studyou.ui.theme.StudyouTheme

@Composable
fun CrosswordRoute(
    fileId: String,
    onClose: () -> Unit,
    onFinished: (GameResultArgs) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: CrosswordViewModel = viewModel(
        key = "crossword-$fileId",
        factory = viewModelFactory {
            initializer { CrosswordViewModel(fileId, container.fileRepository, container.gameRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.resultArgs) {
        uiState.resultArgs?.let(onFinished)
    }

    CrosswordScreen(
        uiState = uiState,
        onClose = onClose,
        onLetterTap = viewModel::onLetterTap,
        onClear = viewModel::onClear,
        onShuffle = viewModel::onShuffle,
        onConfirm = viewModel::onConfirmWord,
        onFinishClick = viewModel::onFinishClick
    )
}

@Composable
fun CrosswordScreen(
    uiState: CrosswordUiState,
    onClose: () -> Unit,
    onLetterTap: (Int) -> Unit,
    onClear: () -> Unit,
    onShuffle: () -> Unit,
    onConfirm: () -> Unit,
    onFinishClick: () -> Unit = {}
) {
    Scaffold(topBar = { CloseTopBar("${GameType.CROSSWORD.displayTitle()} — ${uiState.subjectTitle}", onClose) }) { padding ->
        if (uiState.isUnavailable) {
            GameUnavailableMessage(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                CrosswordGrid(uiState = uiState)

                Spacer(modifier = Modifier.height(16.dp))

                CrosswordClues(uiState = uiState)

                Spacer(modifier = Modifier.height(20.dp))

                if (uiState.isComplete) {
                    // The completed grid stays visible until the player decides to leave,
                    // instead of jumping straight to the result screen.
                    Text(
                        text = "Você completou as palavras cruzadas! " +
                            "${uiState.entries.size} de ${uiState.entries.size} palavras encontradas.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AprendixColors.StatusSuccess
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(text = "Finalizar", onClick = onFinishClick, enabled = uiState.pendingResultArgs != null)
                } else {
                    Text(
                        text = "${uiState.solvedEntries.size} de ${uiState.entries.size} palavras encontradas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CurrentWordBanner(
                        word = uiState.currentWord,
                        placeholder = "Toque nas letras para formar uma palavra",
                        isError = uiState.lastAttemptCorrect == false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LetterTileGrid(
                        letters = uiState.letters,
                        selectedIndices = uiState.selectedIndices,
                        onLetterTap = onLetterTap
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        SecondaryButton(text = "Embaralhar", onClick = onShuffle, modifier = Modifier.weight(1f))
                        SecondaryButton(text = "Limpar", onClick = onClear, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(text = "Confirmar palavra", onClick = onConfirm, enabled = uiState.currentWord.length >= 3)
                }

                if (uiState.bonusWordsFound.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Palavras bônus (${uiState.bonusWordsFound.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.bonusWordsFound.forEach { word ->
                            LabelChip(
                                text = word,
                                containerColor = AprendixColors.StatusWarningContainer,
                                contentColor = AprendixColors.StatusWarning
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrosswordGrid(uiState: CrosswordUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until uiState.size) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until uiState.size) {
                    val cell = row to col
                    if (cell !in uiState.gridCells) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val letter = uiState.cellLetters[cell]
                        GridCell(
                            letter = letter?.toString().orEmpty(),
                            filled = letter != null,
                            number = uiState.startCellNumbers[cell],
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCell(letter: String, filled: Boolean, number: Int?, modifier: Modifier) {
    val backgroundColor = if (filled) AprendixColors.StatusSuccessContainer else MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, fontWeight = FontWeight.Bold, color = if (filled) AprendixColors.StatusSuccess else Color.Transparent)
        if (number != null) {
            // Matches the number of this word's clue in the list below the grid.
            Text(
                text = number.toString(),
                fontSize = 8.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 1.dp)
            )
        }
    }
}

/** The clue of every word on the grid, grouped like a crossword: across, then down. */
@Composable
private fun CrosswordClues(uiState: CrosswordUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            CrosswordDirection.ACROSS to "Horizontais",
            CrosswordDirection.DOWN to "Verticais"
        ).forEach { (direction, label) ->
            val entries = uiState.entries
                .filter { it.direction == direction }
                .sortedBy { uiState.numberOf(it) }
            if (entries.isEmpty()) return@forEach

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                entries.forEach { entry ->
                    ClueLine(
                        number = uiState.numberOf(entry),
                        entry = entry,
                        solved = entry in uiState.solvedEntries
                    )
                }
            }
        }
    }
}

@Composable
private fun ClueLine(number: Int, entry: CrosswordEntry, solved: Boolean) {
    val hint = entry.clue.ifBlank { "Palavra do conteúdo" }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (solved) AprendixColors.StatusSuccess else MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (solved) {
                "${entry.answer} — $hint"
            } else {
                "$hint (${entry.answer.length} letras)"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (solved) FontWeight.SemiBold else FontWeight.Normal,
            color = if (solved) AprendixColors.StatusSuccess else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CrosswordScreenPreview() {
    StudyouTheme {
        CrosswordScreen(
            uiState = CrosswordUiState(
                subjectTitle = "Sistema Solar",
                size = 5,
                letters = listOf('M', 'A', 'R', 'T', 'E', 'S', 'O', 'L'),
                selectedIndices = listOf(0, 1),
                entries = listOf(
                    CrosswordEntry(CrosswordDirection.DOWN, "TERRA", 0, 2),
                    CrosswordEntry(CrosswordDirection.ACROSS, "MARTE", 2, 0, clue = "Planeta vermelho")
                ),
                isLoading = false
            ),
            onClose = {},
            onLetterTap = {},
            onClear = {},
            onShuffle = {},
            onConfirm = {}
        )
    }
}
