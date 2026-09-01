package br.com.studyou.ui.screens.games.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import br.com.studyou.domain.model.QuizQuestion
import br.com.studyou.ui.components.CloseTopBar
import br.com.studyou.ui.components.GameUnavailableMessage
import br.com.studyou.ui.components.PrimaryButton
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.screens.result.GameResultArgs
import br.com.studyou.ui.theme.StudyouTheme

@Composable
fun QuizRoute(
    fileId: String,
    onClose: () -> Unit,
    onFinished: (GameResultArgs) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: QuizViewModel = viewModel(
        key = "quiz-$fileId",
        factory = viewModelFactory {
            initializer { QuizViewModel(fileId, container.fileRepository, container.gameRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.resultArgs) {
        uiState.resultArgs?.let(onFinished)
    }

    QuizScreen(
        uiState = uiState,
        onClose = onClose,
        onSelectOption = viewModel::selectOption,
        onConfirm = viewModel::confirmAnswer
    )
}

@Composable
fun QuizScreen(
    uiState: QuizUiState,
    onClose: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    Scaffold(topBar = { CloseTopBar(uiState.progressLabel, onClose) }) { padding ->
        val question = uiState.currentQuestion
        if (!uiState.isLoading && question == null) {
            GameUnavailableMessage(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (question != null) {
                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                question.options.forEachIndexed { index, option ->
                    OptionRow(
                        text = option,
                        selected = uiState.selectedOptionIndex == index,
                        onClick = { onSelectOption(index) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = if (uiState.selectedOptionIndex != null) "Confirmar" else "Selecione uma resposta",
                onClick = onConfirm,
                enabled = uiState.selectedOptionIndex != null
            )
        }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = text, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun QuizScreenPreview() {
    StudyouTheme {
        QuizScreen(
            uiState = QuizUiState(
                subjectTitle = "Sistema Solar",
                questions = listOf(
                    QuizQuestion("q1", "Qual planeta é conhecido como o Planeta Vermelho?", listOf("Vênus", "Marte", "Júpiter", "Netuno"), 1)
                ),
                currentIndex = 0,
                isLoading = false
            ),
            onClose = {},
            onSelectOption = {},
            onConfirm = {}
        )
    }
}
