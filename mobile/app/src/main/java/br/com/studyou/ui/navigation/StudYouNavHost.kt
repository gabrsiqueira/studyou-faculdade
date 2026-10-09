package br.com.studyou.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.screens.auth.LoginRoute
import br.com.studyou.ui.screens.files.FileListRoute
import br.com.studyou.ui.screens.games.GamesListRoute
import br.com.studyou.ui.screens.games.crossword.CrosswordRoute
import br.com.studyou.ui.screens.games.flashcards.FlashcardsRoute
import br.com.studyou.ui.screens.games.guessword.GuessWordRoute
import br.com.studyou.ui.screens.games.quiz.QuizRoute
import br.com.studyou.ui.screens.history.HistoryRoute
import br.com.studyou.ui.screens.profile.ProfileRoute
import br.com.studyou.ui.screens.result.GameResultArgs
import br.com.studyou.ui.screens.result.ResultScreen
import br.com.studyou.ui.screens.upload.FileUploadRoute

@Composable
fun StudYouNavHost() {
    val authRepository = LocalAppContainer.current.authRepository
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // A user who is still signed in from a previous run goes straight to their files.
    val startDestination = remember { if (authRepository.isLoggedIn()) Screen.Files.route else Screen.Login.route }

    // Logging out, or the backend rejecting an expired token, ends the session: send the
    // user back to the login screen with nothing left on the back stack.
    val isLoggedIn by authRepository.observeIsLoggedIn().collectAsState(initial = authRepository.isLoggedIn())
    var wasLoggedIn by remember { mutableStateOf(isLoggedIn) }
    LaunchedEffect(isLoggedIn) {
        if (wasLoggedIn && !isLoggedIn) {
            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
        }
        wasLoggedIn = isLoggedIn
    }

    fun navigateToBottomNavScreen(screen: Screen) = navController.navigateToTopLevel(screen)

    NavHost(navController = navController, startDestination = startDestination) {
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
                onNavigateBottomNav = ::navigateToBottomNavScreen
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
            navController.navigate(Screen.Result.createRoute(args))
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

        composable(route = Screen.Result.route, arguments = Screen.Result.arguments) { entry ->
            val args = Screen.Result.readArgs(entry.arguments)

            ResultScreen(
                title = args.title,
                big = args.big,
                caption = args.caption,
                xp = args.xp,
                streak = args.streak,
                note = args.note,
                level = args.level,
                leveledUp = args.leveledUp,
                onClose = { navController.popBackStack(Screen.Files.route, inclusive = false) },
                onPlayAgain = { navController.playAgain(args.fileId, args.gameType) },
                onViewHistory = { navController.openHistoryFromResult() }
            )
        }
    }
}
