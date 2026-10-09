package br.com.studyou.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import br.com.studyou.domain.model.GameAttempt
import br.com.studyou.domain.model.GameHistoryGroup
import br.com.studyou.domain.model.GameType
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.navigation.Screen
import br.com.studyou.ui.navigation.StudYouBottomNavBar
import br.com.studyou.ui.screens.games.gameTypeStyle
import br.com.studyou.ui.theme.StudyouTheme

@Composable
fun HistoryRoute(currentRoute: String?, onNavigateBottomNav: (Screen) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HistoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HistoryViewModel(container.historyRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    HistoryScreen(
        uiState = uiState,
        currentRoute = currentRoute,
        onNavigateBottomNav = onNavigateBottomNav,
        onToggleExpand = viewModel::onToggleExpand
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    currentRoute: String?,
    onNavigateBottomNav: (Screen) -> Unit,
    onToggleExpand: (GameType) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text("Meu histórico", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Todos os jogos que você já fez",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = { StudYouBottomNavBar(currentRoute, onNavigateBottomNav) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(uiState.groups, key = { it.gameType }) { group ->
                HistoryGroupCard(
                    group = group,
                    expanded = uiState.expandedGameType == group.gameType,
                    onToggle = { onToggleExpand(group.gameType) }
                )
            }
        }
    }
}

@Composable
private fun HistoryGroupCard(group: GameHistoryGroup, expanded: Boolean, onToggle: () -> Unit) {
    val style = gameTypeStyle(group.gameType)
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = RoundedCornerShape(10.dp), color = style.containerColor) {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(style.icon, contentDescription = null, tint = style.contentColor)
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(group.title, fontWeight = FontWeight.Bold)
                    Text(
                        group.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    group.attempts.forEach { attempt -> AttemptRow(attempt) }
                }
            }
        }
    }
}

@Composable
private fun AttemptRow(attempt: GameAttempt) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(attempt.playedAtLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(attempt.scoreLabel, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    StudyouTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                groups = listOf(
                    GameHistoryGroup(
                        GameType.QUIZ,
                        "Quiz — Sistema Solar",
                        "Melhor nota: 9/10 · 3 tentativas",
                        listOf(GameAttempt("a1", "file-1", GameType.QUIZ, "Sistema Solar", System.currentTimeMillis(), "9/10", 90, 90, "9/10"))
                    )
                ),
                expandedGameType = GameType.QUIZ
            ),
            currentRoute = Screen.History.route,
            onNavigateBottomNav = {},
            onToggleExpand = {}
        )
    }
}
