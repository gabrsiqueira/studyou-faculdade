package br.com.studyou.ui.screens.games

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Style
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.studyou.domain.model.GameType
import br.com.studyou.ui.theme.AprendixColors

/** Icon and accent colors used to represent each [GameType] across the games list and result screen. */
data class GameTypeStyle(val icon: ImageVector, val containerColor: Color, val contentColor: Color)

@Composable
fun gameTypeStyle(type: GameType): GameTypeStyle = when (type) {
    GameType.QUIZ -> GameTypeStyle(
        Icons.AutoMirrored.Filled.HelpOutline,
        AprendixColors.BrandSurfaceTint,
        AprendixColors.BrandPrimary
    )

    GameType.FLASHCARDS -> GameTypeStyle(
        Icons.Filled.Style,
        AprendixColors.StatusWarningContainer,
        AprendixColors.StatusWarning
    )

    GameType.GUESS_WORD -> GameTypeStyle(
        Icons.Filled.Psychology,
        AprendixColors.StatusInfoContainer,
        AprendixColors.StatusInfo
    )

    GameType.CROSSWORD -> GameTypeStyle(
        Icons.Filled.GridOn,
        AprendixColors.StatusSuccessContainer,
        AprendixColors.StatusSuccess
    )
}
