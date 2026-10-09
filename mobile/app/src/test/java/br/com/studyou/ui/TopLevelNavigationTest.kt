package br.com.studyou.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.studyou.domain.model.GameType
import br.com.studyou.ui.navigation.Screen
import br.com.studyou.ui.navigation.navigateToTopLevel
import br.com.studyou.ui.navigation.openHistoryFromResult
import br.com.studyou.ui.navigation.playAgain
import br.com.studyou.ui.screens.result.GameResultArgs
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercises the real navigation helpers on a graph with the app's real routes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TopLevelNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController

    private val fileArg = listOf(navArgument("fileId") { type = NavType.StringType })

    private fun setUpGraph() {
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = Screen.Login.route) {
                composable(Screen.Login.route) { Text("login") }
                composable(Screen.Files.route) { Text("files") }
                composable(Screen.History.route) { Text("history") }
                composable(Screen.Profile.route) { Text("profile") }
                composable(Screen.GamesList.route, arguments = fileArg) { Text("games") }
                composable(Screen.Quiz.route, arguments = fileArg) { Text("quiz") }
                composable(Screen.Result.route, arguments = Screen.Result.arguments) { Text("result") }
            }
        }
        // Same as a successful login: Files replaces Login.
        composeRule.runOnIdle {
            navController.navigate(Screen.Files.route) { popUpTo(Screen.Login.route) { inclusive = true } }
        }
    }

    private fun finishQuizOn(fileId: String) = composeRule.runOnIdle {
        navController.navigate(Screen.GamesList.createRoute(fileId))
        navController.navigate(Screen.Quiz.createRoute(fileId))
        navController.navigate(
            Screen.Result.createRoute(GameResultArgs(fileId, GameType.QUIZ, "Quiz", "4/5", "corretas", 40, 1, ""))
        )
    }

    private fun currentRoute() = composeRule.runOnIdle { navController.currentDestination?.route }

    private fun backStackRoutes() = composeRule.runOnIdle {
        navController.currentBackStack.value.mapNotNull { it.destination.route }.filter { it.isNotEmpty() }
    }

    @Test
    fun afterViewingHistoryFromAResult_theFilesTabStillOpensFiles() {
        setUpGraph()
        finishQuizOn("file-1")
        composeRule.runOnIdle { navController.openHistoryFromResult() }
        assertEquals(Screen.History.route, currentRoute())

        composeRule.runOnIdle { navController.navigateToTopLevel(Screen.Files) }

        assertEquals(Screen.Files.route, currentRoute())
    }

    @Test
    fun switchingTabsBackAndForth_alwaysLandsOnTheChosenTab() {
        setUpGraph()
        for (tab in listOf(Screen.History, Screen.Files, Screen.Profile, Screen.Files, Screen.History, Screen.Profile)) {
            composeRule.runOnIdle { navController.navigateToTopLevel(tab) }
            assertEquals(tab.route, currentRoute())
        }
    }

    @Test
    fun playAgain_goesBackToThatFilesGamesAndStartsTheGame() {
        setUpGraph()
        finishQuizOn("file-1")

        composeRule.runOnIdle { navController.playAgain("file-1", GameType.QUIZ) }

        assertEquals(listOf(Screen.Files.route, Screen.GamesList.route, Screen.Quiz.route), backStackRoutes())
    }
}
