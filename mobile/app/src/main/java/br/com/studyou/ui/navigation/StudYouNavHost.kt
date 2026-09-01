package br.com.studyou.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.studyou.domain.model.GameType
import br.com.studyou.ui.screens.auth.LoginRoute
import br.com.studyou.ui.screens.files.FileListRoute
import br.com.studyou.ui.screens.games.GamesListRoute
import br.com.studyou.ui.screens.games.crossword.CrosswordRoute
import br.com.studyou.ui.screens.games.flashcards.FlashcardsRoute
import br.com.studyou.ui.screens.games.guessword.GuessWordRoute
import br.com.studyou.ui.screens.games.quiz.QuizRoute
import br.com.studyou.ui.screens.games.wordforming.WordFormingRoute
import br.com.studyou.ui.screens.history.HistoryRoute
import br.com.studyou.ui.screens.profile.ProfileRoute
import br.com.studyou.ui.screens.result.GameResultArgs
import br.com.studyou.ui.screens.result.ResultScreen
import br.com.studyou.ui.screens.upload.FileUploadRoute
import java.net.URLDecoder

private fun decode(value: String) = URLDecoder.decode(value, "UTF-8")

@Composable
fun StudYouNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    fun navigateToBottomNavScreen(screen: Screen) {
        navController.navigate(screen.route) {
            popUpTo(Screen.Files.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginRoute(
                onAuthenticated = {
                    navController.navigate(Screen.Files.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Files.route) {
            FileListRoute(
                currentRoute = currentRoute,
                onNavigateBottomNav = ::navigateToBottomNavScreen,
                onUploadClick = { navController.navigate(Screen.Upload.route) },
                onFileClick = { file -> navController.navigate(Screen.GamesList.createRoute(file.id)) }
            )
        }

        composable(Screen.History.route) {
            HistoryRoute(currentRoute = currentRoute, onNavigateBottomNav = ::navigateToBottomNavScreen)
        }

        composable(Screen.Profile.route) {
            ProfileRoute(
                currentRoute = currentRoute,
                onNavigateBottomNav = ::navigateToBottomNavScreen,
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Upload.route) {
            FileUploadRoute(
                onBack = { navController.popBackStack() },
                onUploaded = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GamesList.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { entry ->
            val fileId = entry.arguments?.getString("fileId").orEmpty()
            GamesListRoute(
                fileId = fileId,
                onBack = { navController.popBackStack() },
                onGameClick = { gameType -> navController.navigate(gameRouteFor(gameType, fileId)) }
            )
        }

        fun onGameFinished(args: GameResultArgs) {
            navController.navigate(
                Screen.Result.createRoute(
                    fileId = args.fileId,
                    gameType = args.gameType,
                    title = args.title,
                    big = args.big,
                    caption = args.caption,
                    xp = args.xp,
                    streak = args.streak,
                    note = args.note
                )
            )
        }

        composable(
            route = Screen.Quiz.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { entry ->
            val fileId = entry.arguments?.getString("fileId").orEmpty()
            QuizRoute(fileId = fileId, onClose = { navController.popBackStack() }, onFinished = ::onGameFinished)
        }

        composable(
            route = Screen.Flashcards.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { entry ->
            val fileId = entry.arguments?.getString("fileId").orEmpty()
            FlashcardsRoute(fileId = fileId, onClose = { navController.popBackStack() }, onFinished = ::onGameFinished)
        }

        composable(
            route = Screen.GuessWord.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { entry ->
            val fileId = entry.arguments?.getString("fileId").orEmpty()
            GuessWordRoute(fileId = fileId, onClose = { navController.popBackStack() }, onFinished = ::onGameFinished)
        }

        composable(
            route = Screen.Crossword.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { entry ->
            val fileId = entry.arguments?.getString("fileId").orEmpty()
            CrosswordRoute(fileId = fileId, onClose = { navController.popBackStack() }, onFinished = ::onGameFinished)
        }

        composable(
            route = Screen.WordForming.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { entry ->
            val fileId = entry.arguments?.getString("fileId").orEmpty()
            WordFormingRoute(fileId = fileId, onClose = { navController.popBackStack() }, onFinished = ::onGameFinished)
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("fileId") { type = NavType.StringType },
                navArgument("gameType") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("big") { type = NavType.StringType; defaultValue = "" },
                navArgument("caption") { type = NavType.StringType; defaultValue = "" },
                navArgument("xp") { type = NavType.IntType; defaultValue = 0 },
                navArgument("streak") { type = NavType.IntType; defaultValue = 0 },
                navArgument("note") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val fileId = entry.arguments?.getString("fileId").orEmpty()
            val gameType = runCatching {
                GameType.valueOf(entry.arguments?.getString("gameType").orEmpty())
            }.getOrDefault(GameType.QUIZ)

            ResultScreen(
                title = decode(entry.arguments?.getString("title").orEmpty()),
                big = decode(entry.arguments?.getString("big").orEmpty()),
                caption = decode(entry.arguments?.getString("caption").orEmpty()),
                xp = entry.arguments?.getInt("xp") ?: 0,
                streak = entry.arguments?.getInt("streak") ?: 0,
                note = decode(entry.arguments?.getString("note").orEmpty()),
                onClose = { navController.popBackStack(Screen.Files.route, inclusive = false) },
                onPlayAgain = {
                    navController.popBackStack(Screen.GamesList.createRoute(fileId), inclusive = false)
                    navController.navigate(gameRouteFor(gameType, fileId))
                },
                onViewHistory = {
                    navController.popBackStack(Screen.Files.route, inclusive = false)
                    navController.navigate(Screen.History.route)
                }
            )
        }
    }
}
