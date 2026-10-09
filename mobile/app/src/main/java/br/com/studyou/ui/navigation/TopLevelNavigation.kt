package br.com.studyou.ui.navigation

import androidx.navigation.NavController
import br.com.studyou.domain.model.GameType

/**
 * Switches between the bottom navigation tabs (Arquivos, Histórico, Perfil). The back stack is
 * always just Arquivos, plus the selected tab on top of it.
 *
 * No saveState/restoreState: the tabs are single screens with nothing to preserve, and since
 * Arquivos is the popUpTo target, restoring "its" saved state brought back the tab that had
 * just been popped — tapping Arquivos after opening Histórico from a result kept you there.
 */
fun NavController.navigateToTopLevel(screen: Screen) {
    if (screen == Screen.Files && popBackStack(Screen.Files.route, inclusive = false)) return
    navigate(screen.route) {
        popUpTo(Screen.Files.route)
        launchSingleTop = true
    }
}

/** From a game's result screen: back to that file's games list, then straight into [gameType] again. */
fun NavController.playAgain(fileId: String, gameType: GameType) {
    popBackStack(Screen.GamesList.createRoute(fileId), inclusive = false)
    navigate(gameRouteFor(gameType, fileId))
}

/** From a game's result screen: open the history tab. */
fun NavController.openHistoryFromResult() = navigateToTopLevel(Screen.History)
