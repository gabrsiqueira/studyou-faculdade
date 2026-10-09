package br.com.studyou.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.studyou.domain.model.GameType
import br.com.studyou.ui.navigation.Screen
import br.com.studyou.ui.screens.result.GameResultArgs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Navigates through the real Result route definition and checks every value survives intact. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ResultRouteTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resultRoute_roundTripsTextWithPercentPlusAndAccents() {
        val sent = GameResultArgs(
            fileId = "file-1",
            gameType = GameType.FLASHCARDS,
            title = "Flashcards — Fotossíntese & Cia",
            big = "80%",
            caption = "lembrados",
            xp = 32,
            streak = 2,
            note = "Você revisou 5 cartões + lembrou de 4 (80%)."
        )
        var received: GameResultArgs? = null

        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "start") {
                composable("start") {
                    LaunchedEffect(Unit) { navController.navigate(Screen.Result.createRoute(sent)) }
                }
                composable(route = Screen.Result.route, arguments = Screen.Result.arguments) { entry ->
                    val args = Screen.Result.readArgs(entry.arguments)
                    received = args
                    Text(args.big)
                }
            }
        }

        composeRule.onNodeWithText("80%").assertExists()
        assert(received == sent) { "expected $sent but got $received" }
    }
}
