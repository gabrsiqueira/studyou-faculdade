package br.com.studyou.ui.screens.result

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.studyou.ui.components.CloseTopBar
import br.com.studyou.ui.components.LabelChip
import br.com.studyou.ui.components.PrimaryButton
import br.com.studyou.ui.components.SecondaryButton
import br.com.studyou.ui.theme.AprendixColors
import br.com.studyou.ui.theme.StudyouTheme

@Composable
fun ResultScreen(
    title: String,
    big: String,
    caption: String,
    xp: Int,
    streak: Int,
    note: String,
    level: Int = 1,
    leveledUp: Boolean = false,
    onClose: () -> Unit,
    onPlayAgain: () -> Unit,
    onViewHistory: () -> Unit
) {
    Scaffold(topBar = { CloseTopBar("Resultado do jogo", onClose) }) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Surface(shape = CircleShape, color = AprendixColors.StatusWarningContainer) {
                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = AprendixColors.StatusWarning,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Muito bem!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(big, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelChip(
                    text = "+$xp XP",
                    containerColor = AprendixColors.StatusWarningContainer,
                    contentColor = AprendixColors.StatusWarning
                )
                LabelChip(
                    text = "Nível $level",
                    containerColor = AprendixColors.StatusSuccessContainer,
                    contentColor = AprendixColors.StatusSuccess
                )
                LabelChip(
                    text = "Sequência de $streak dias",
                    containerColor = AprendixColors.StatusInfoContainer,
                    contentColor = AprendixColors.StatusInfo
                )
            }

            if (leveledUp) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AprendixColors.StatusSuccessContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Você subiu para o nível $level!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AprendixColors.StatusSuccess,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(text = "Jogar novamente", onClick = onPlayAgain)
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(text = "Ver histórico", onClick = onViewHistory)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    StudyouTheme {
        ResultScreen(
            title = "Quiz — Sistema Solar",
            big = "8/10",
            caption = "corretas",
            xp = 80,
            streak = 5,
            note = "Sua melhor nota nesse jogo é 9/10 — quase lá!",
            level = 3,
            leveledUp = true,
            onClose = {},
            onPlayAgain = {},
            onViewHistory = {}
        )
    }
}
