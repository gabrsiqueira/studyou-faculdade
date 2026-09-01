package br.com.studyou.ui.screens.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.studyou.domain.model.GameSummary
import br.com.studyou.domain.model.GameType
import br.com.studyou.ui.components.GameUnavailableMessage
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.theme.StudyouTheme

@Composable
fun GamesListRoute(
    fileId: String,
    onBack: () -> Unit,
    onGameClick: (GameType) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: GamesListViewModel = viewModel(
        key = "games-list-$fileId",
        factory = viewModelFactory {
            initializer { GamesListViewModel(fileId, container.fileRepository, container.gameRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    GamesListScreen(uiState = uiState, onBack = onBack, onGameClick = onGameClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesListScreen(
    uiState: GamesListUiState,
    onBack: () -> Unit,
    onGameClick: (GameType) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text("Jogos disponíveis", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = uiState.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (!uiState.isLoading && uiState.games.isEmpty()) {
            GameUnavailableMessage(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(uiState.games, key = { it.type }) { game ->
                    GameCard(game = game, onClick = { onGameClick(game.type) })
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: GameSummary, onClick: () -> Unit) {
    val style = gameTypeStyle(game.type)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = style.containerColor) {
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(style.icon, contentDescription = null, tint = style.contentColor)
                }
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(game.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = game.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = game.resultLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (game.played) style.contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Button(onClick = onClick) {
                Text("Jogar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GamesListScreenPreview() {
    StudyouTheme {
        GamesListScreen(
            uiState = GamesListUiState(
                fileName = "Sistema Solar – Resumo.pdf",
                games = listOf(
                    GameSummary(GameType.QUIZ, "Quiz", "Perguntas de múltipla escolha sobre o conteúdo do arquivo.", "Melhor nota: 8/10", true),
                    GameSummary(GameType.FLASHCARDS, "Flashcards", "Revise os conceitos principais em cartões de memorização.", "Ainda não jogado", false)
                )
            ),
            onBack = {},
            onGameClick = {}
        )
    }
}
