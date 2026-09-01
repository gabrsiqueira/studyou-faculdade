package br.com.studyou.ui.screens.games.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.studyou.domain.model.Flashcard
import br.com.studyou.ui.components.CloseTopBar
import br.com.studyou.ui.components.GameUnavailableMessage
import br.com.studyou.ui.components.PrimaryButton
import br.com.studyou.ui.components.SecondaryButton
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.screens.result.GameResultArgs
import br.com.studyou.ui.theme.AprendixColors
import br.com.studyou.ui.theme.StudyouTheme

@Composable
fun FlashcardsRoute(
    fileId: String,
    onClose: () -> Unit,
    onFinished: (GameResultArgs) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: FlashcardsViewModel = viewModel(
        key = "flashcards-$fileId",
        factory = viewModelFactory {
            initializer { FlashcardsViewModel(fileId, container.fileRepository, container.gameRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.resultArgs) {
        uiState.resultArgs?.let(onFinished)
    }

    FlashcardsScreen(
        uiState = uiState,
        onClose = onClose,
        onFlip = viewModel::flipCard,
        onMark = viewModel::markCard
    )
}

@Composable
fun FlashcardsScreen(
    uiState: FlashcardsUiState,
    onClose: () -> Unit,
    onFlip: () -> Unit,
    onMark: (Boolean) -> Unit
) {
    Scaffold(topBar = { CloseTopBar(uiState.progressLabel, onClose) }) { padding ->
        if (!uiState.isLoading && uiState.currentCard == null) {
            GameUnavailableMessage(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            val card = uiState.currentCard
            if (card != null) {
                Spacer(modifier = Modifier.height(24.dp))
                FlashcardFace(card = card, isFlipped = uiState.isFlipped, onClick = onFlip)
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(text = "Não lembrei", onClick = { onMark(false) }, modifier = Modifier.weight(1f))
                PrimaryButton(text = "Lembrei", onClick = { onMark(true) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FlashcardFace(card: Flashcard, isFlipped: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f, label = "flashcard-flip")

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            if (rotation <= 90f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "PERGUNTA",
                        color = AprendixColors.BrandPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        card.question,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Toque para virar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                ) {
                    Text(
                        "RESPOSTA",
                        color = AprendixColors.StatusSuccess,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        card.answer,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FlashcardsScreenPreview() {
    StudyouTheme {
        FlashcardsScreen(
            uiState = FlashcardsUiState(
                subjectTitle = "Sistema Solar",
                cards = listOf(Flashcard("f1", "O que é a heliosfera?", "A região do espaço dominada pelo vento solar do Sol.")),
                isLoading = false
            ),
            onClose = {},
            onFlip = {},
            onMark = {}
        )
    }
}
