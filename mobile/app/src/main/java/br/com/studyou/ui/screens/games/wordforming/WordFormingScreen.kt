package br.com.studyou.ui.screens.games.wordforming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
fun WordFormingRoute(
    fileId: String,
    onClose: () -> Unit,
    onFinished: (GameResultArgs) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: WordFormingViewModel = viewModel(
        key = "word-forming-$fileId",
        factory = viewModelFactory {
            initializer { WordFormingViewModel(fileId, container.fileRepository, container.gameRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.resultArgs) {
        uiState.resultArgs?.let(onFinished)
    }

    WordFormingScreen(
        uiState = uiState,
        onClose = onClose,
        onLetterTap = viewModel::onLetterTap,
        onClear = viewModel::onClear,
        onShuffle = viewModel::onShuffle,
        onConfirm = viewModel::onConfirmWord,
        onFinish = viewModel::onFinishGame
    )
}

@Composable
fun WordFormingScreen(
    uiState: WordFormingUiState,
    onClose: () -> Unit,
    onLetterTap: (Int) -> Unit,
    onClear: () -> Unit,
    onShuffle: () -> Unit,
    onConfirm: () -> Unit,
    onFinish: () -> Unit
) {
    Scaffold(topBar = { CloseTopBar("Forma-Palavras — ${uiState.subjectTitle}", onClose) }) { padding ->
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                CurrentWordBanner(
                    word = uiState.currentWord,
                    placeholder = "Toque nas letras para formar uma palavra",
                    isError = false
                )

                Spacer(modifier = Modifier.height(24.dp))

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

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Palavras encontradas (${uiState.foundWords.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.foundWords.forEach { word ->
                        LabelChip(
                            text = word,
                            containerColor = AprendixColors.StatusSuccessContainer,
                            contentColor = AprendixColors.StatusSuccess
                        )
                    }
                }
            }

            TextButton(onClick = onFinish, enabled = uiState.foundWords.isNotEmpty()) {
                Text("Finalizar jogo")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WordFormingScreenPreview() {
    StudyouTheme {
        WordFormingScreen(
            uiState = WordFormingUiState(
                subjectTitle = "Sistema Solar",
                letters = listOf('M', 'A', 'R', 'T', 'E', 'S', 'O', 'L'),
                selectedIndices = listOf(5, 6),
                foundWords = listOf("SOL", "MAR"),
                isLoading = false
            ),
            onClose = {},
            onLetterTap = {},
            onClear = {},
            onShuffle = {},
            onConfirm = {},
            onFinish = {}
        )
    }
}
