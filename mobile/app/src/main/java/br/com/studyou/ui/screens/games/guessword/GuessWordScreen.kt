package br.com.studyou.ui.screens.games.guessword

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.studyou.ui.components.CloseTopBar
import br.com.studyou.ui.components.GameUnavailableMessage
import br.com.studyou.ui.components.OnScreenKeyboard
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.screens.result.GameResultArgs
import br.com.studyou.ui.theme.AprendixColors
import br.com.studyou.ui.theme.StudyouTheme

@Composable
fun GuessWordRoute(
    fileId: String,
    onClose: () -> Unit,
    onFinished: (GameResultArgs) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: GuessWordViewModel = viewModel(
        key = "guess-word-$fileId",
        factory = viewModelFactory {
            initializer { GuessWordViewModel(fileId, container.fileRepository, container.gameRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.resultArgs) {
        uiState.resultArgs?.let(onFinished)
    }

    GuessWordScreen(
        uiState = uiState,
        onClose = onClose,
        onLetterClick = viewModel::onLetterTyped,
        onBackspaceClick = viewModel::onBackspace,
        onSubmitClick = viewModel::onSubmitGuess,
        onFinishClick = viewModel::onFinishClick
    )
}

@Composable
fun GuessWordScreen(
    uiState: GuessWordUiState,
    onClose: () -> Unit,
    onLetterClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onFinishClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CloseTopBar("Termo — ${uiState.subjectTitle}", onClose)
        }
    ) { padding ->
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
            Text(
                text = when {
                    uiState.isSolved -> "Você acertou em ${uiState.attemptsUsed} de ${uiState.maxAttempts} tentativas!"
                    uiState.isFailed -> "Não foi dessa vez. A palavra era \"${uiState.revealedAnswer}\"."
                    else -> uiState.attemptLabel
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (uiState.isOver) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    uiState.isSolved -> AprendixColors.StatusSuccess
                    uiState.isFailed -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // The full board is always shown, like Termo: rows already played, the row
            // currently being typed, and empty rows for the attempts still to come. It only
            // takes the space the keyboard leaves free, with cells sized to fit both the
            // width and the height, so short words never push the keyboard off screen.
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val columns = uiState.answerLength.coerceAtLeast(1)
                val rows = uiState.maxAttempts.coerceAtLeast(1)
                val cellSize = min(
                    min(
                        (maxWidth - BOARD_GAP * (columns - 1)) / columns,
                        (maxHeight - BOARD_GAP * (rows - 1)) / rows
                    ),
                    MAX_CELL_SIZE
                ).coerceAtLeast(0.dp)

                Column(verticalArrangement = Arrangement.spacedBy(BOARD_GAP)) {
                    repeat(uiState.maxAttempts) { index ->
                        when {
                            index < uiState.guesses.size ->
                                GuessRow(row = uiState.guesses[index], cellSize = cellSize)
                            index == uiState.guesses.size && !uiState.isSolved && !uiState.isFailed ->
                                CurrentInputRow(input = uiState.currentInput, length = uiState.answerLength, cellSize = cellSize)
                            else ->
                                CurrentInputRow(input = "", length = uiState.answerLength, cellSize = cellSize)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isOver) {
                // The finished board (and the colored keyboard) stays visible until the player
                // decides to leave, instead of jumping straight to the result screen.
                Button(
                    onClick = onFinishClick,
                    enabled = uiState.pendingResultArgs != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text("Finalizar", fontWeight = FontWeight.SemiBold)
                }
            }

            val letterColors = uiState.keyboardStates.mapValues { (_, state) -> colorFor(state) }
            OnScreenKeyboard(
                letterColors = letterColors,
                onLetterClick = onLetterClick,
                onBackspaceClick = onBackspaceClick,
                onEnterClick = onSubmitClick,
                enterEnabled = !uiState.isOver && uiState.currentInput.length == uiState.answerLength
            )
        }
    }
}

private val BOARD_GAP = 6.dp
private val MAX_CELL_SIZE = 64.dp

@Composable
private fun GuessRow(row: List<Pair<Char, LetterState>>, cellSize: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(BOARD_GAP)) {
        row.forEach { (letter, state) ->
            LetterCell(
                letter = letter.toString(),
                backgroundColor = colorFor(state),
                textColor = Color.White,
                modifier = Modifier.size(cellSize)
            )
        }
    }
}

@Composable
private fun CurrentInputRow(input: String, length: Int, cellSize: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(BOARD_GAP)) {
        repeat(length) { index ->
            val letter = input.getOrNull(index)?.toString().orEmpty()
            LetterCell(
                letter = letter,
                backgroundColor = MaterialTheme.colorScheme.surface,
                textColor = MaterialTheme.colorScheme.onSurface,
                bordered = true,
                modifier = Modifier.size(cellSize)
            )
        }
    }
}

@Composable
private fun LetterCell(
    letter: String,
    backgroundColor: Color,
    textColor: Color,
    bordered: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .then(
                if (bordered) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
    ) {
        Text(letter, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

private fun colorFor(state: LetterState): Color = when (state) {
    LetterState.CORRECT -> AprendixColors.StatusSuccess
    LetterState.PRESENT -> AprendixColors.StatusWarning
    LetterState.ABSENT -> Color(0xFF9AA0A6)
}

@Preview(showBackground = true)
@Composable
private fun GuessWordScreenPreview() {
    StudyouTheme {
        GuessWordScreen(
            uiState = GuessWordUiState(
                subjectTitle = "Sistema Solar",
                answerLength = 5,
                maxAttempts = 6,
                guesses = listOf(
                    "TERRA".toList().zip(evaluateGuess("TERRA", "MARTE"))
                ),
                currentInput = "PA",
                isLoading = false
            ),
            onClose = {},
            onLetterClick = {},
            onBackspaceClick = {},
            onSubmitClick = {}
        )
    }
}
