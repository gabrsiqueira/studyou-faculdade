package br.com.studyou.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import br.com.studyou.domain.model.LevelProgress
import br.com.studyou.domain.model.UserProfile
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.navigation.Screen
import br.com.studyou.ui.navigation.StudYouBottomNavBar
import br.com.studyou.ui.theme.AprendixColors
import br.com.studyou.ui.theme.StudyouTheme

@Composable
fun ProfileRoute(
    currentRoute: String?,
    onNavigateBottomNav: (Screen) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ProfileViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ProfileViewModel(container.userRepository, container.historyRepository, container.authRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    ProfileScreen(
        uiState = uiState,
        currentRoute = currentRoute,
        onNavigateBottomNav = onNavigateBottomNav,
        onLogoutClick = viewModel::logout
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    currentRoute: String?,
    onNavigateBottomNav: (Screen) -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(bottomBar = { StudYouBottomNavBar(currentRoute, onNavigateBottomNav) }) { padding ->
        val profile = uiState.profile
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = profile?.name?.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(profile?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(profile?.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp))

            LevelCard(progress = (profile ?: EMPTY_PROFILE).levelProgress)

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(value = "${profile?.completedGames ?: 0}", label = "jogos concluídos", modifier = Modifier.weight(1f))
                StatCard(value = "${profile?.averageScorePercent ?: 0}%", label = "nota média", modifier = Modifier.weight(1f))
                StatCard(value = "${profile?.streakDays ?: 0}", label = "dias seguidos", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
                Text("Sair", color = AprendixColors.StatusDanger, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private val EMPTY_PROFILE = UserProfile(name = "", email = "", completedGames = 0, averageScorePercent = 0, streakDays = 0)

/** The player's level, total XP and how much is left to reach the next level. */
@Composable
private fun LevelCard(progress: LevelProgress) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Nível ${progress.level}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${progress.totalXp} XP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AprendixColors.StatusWarning
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "${progress.xpIntoLevel}/${progress.xpForNextLevel} XP — faltam ${progress.xpToNextLevel} XP para o nível ${progress.level + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    StudyouTheme {
        ProfileScreen(
            uiState = ProfileUiState(profile = UserProfile("Fred", "fred@email.com", 14, 82, 5, totalXp = 420)),
            currentRoute = Screen.Profile.route,
            onNavigateBottomNav = {},
            onLogoutClick = {}
        )
    }
}
