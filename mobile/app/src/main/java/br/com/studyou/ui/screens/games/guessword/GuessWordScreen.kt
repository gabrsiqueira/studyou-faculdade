package br.com.studyou.ui.screens.games.guessword

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
        onSubmitClick = viewModel::onSubmitGuess
    )
}

@Composable
fun GuessWordScreen(
    uiState: GuessWordUiState,
    onClose: () -> Unit,
    onLetterClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onSubmitClick: () -> Unit
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
                text = uiState.attemptLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // The full board is always shown, like Termo: rows already played, the row
            // currently being typed, and empty rows for the attempts still to come.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(uiState.maxAttempts) { index ->
                    when {
                        index < uiState.guesses.size ->
                            GuessRow(row = uiState.guesses[index], length = uiState.answerLength)
                        index == uiState.guesses.size && !uiState.isSolved && !uiState.isFailed ->
                            CurrentInputRow(input = uiState.currentInput, length = uiState.answerLength)
                        else ->
                            CurrentInputRow(input = "", length = uiState.answerLength)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val letterColors = uiState.keyboardStates.mapValues { (_, state) -> colorFor(state) }
            OnScreenKeyboard(
                letterColors = letterColors,
                onLetterClick = onLetterClick,
                onBackspaceClick = onBackspaceClick,
                onEnterClick = onSubmitClick,
                enterEnabled = uiState.currentInput.length == uiState.answerLength
            )
        }
    }
}

@Composable
private fun GuessRow(row: List<Pair<Char, LetterState>>, length: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        row.forEach { (letter, state) ->
            LetterCell(
                letter = letter.toString(),
                backgroundColor = colorFor(state),
                textColor = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CurrentInputRow(input: String, length: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(length) { index ->
            val letter = input.getOrNull(index)?.toString().orEmpty()
            LetterCell(
                letter = letter,
                backgroundColor = MaterialTheme.colorScheme.surface,
                textColor = MaterialTheme.colorScheme.onSurface,
                bordered = true,
                modifier = Modifier.weight(1f)
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
            .aspectRatio(1f)
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
