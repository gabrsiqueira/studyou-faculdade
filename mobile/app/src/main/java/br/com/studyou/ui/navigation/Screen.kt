package br.com.studyou.ui.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import br.com.studyou.domain.model.GameType
import br.com.studyou.ui.screens.result.GameResultArgs

/** All navigable destinations of the app and their route builders. */
sealed class Screen(val route: String) {

    object Login : Screen("login")

    object Files : Screen("files")
    object History : Screen("history")
    object Profile : Screen("profile")

    object Upload : Screen("upload")

    object GamesList : Screen("games/{fileId}") {
        fun createRoute(fileId: String) = "games/$fileId"
    }

    object Quiz : Screen("quiz/{fileId}") {
        fun createRoute(fileId: String) = "quiz/$fileId"
    }

    object Flashcards : Screen("flashcards/{fileId}") {
        fun createRoute(fileId: String) = "flashcards/$fileId"
    }

    object GuessWord : Screen("guess-word/{fileId}") {
        fun createRoute(fileId: String) = "guess-word/$fileId"
    }

    object Crossword : Screen("crossword/{fileId}") {
        fun createRoute(fileId: String) = "crossword/$fileId"
    }

    /** Generic result screen, fed with everything it needs to render via query parameters. */
    object Result : Screen(
        "result/{fileId}/{gameType}?title={title}&big={big}&caption={caption}" +
            "&xp={xp}&streak={streak}&note={note}&level={level}&leveledUp={leveledUp}"
    ) {
        val arguments: List<NamedNavArgument> = listOf(
            navArgument("fileId") { type = NavType.StringType },
            navArgument("gameType") { type = NavType.StringType },
            navArgument("title") { type = NavType.StringType; defaultValue = "" },
            navArgument("big") { type = NavType.StringType; defaultValue = "" },
            navArgument("caption") { type = NavType.StringType; defaultValue = "" },
            navArgument("xp") { type = NavType.IntType; defaultValue = 0 },
            navArgument("streak") { type = NavType.IntType; defaultValue = 0 },
            navArgument("note") { type = NavType.StringType; defaultValue = "" },
            navArgument("level") { type = NavType.IntType; defaultValue = 1 },
            navArgument("leveledUp") { type = NavType.BoolType; defaultValue = false }
        )

        // Uri.encode (not URLEncoder): Navigation decodes these back on its own, so the values
        // must not be decoded a second time — doing so crashed on any literal "%" (e.g. the
        // Flashcards "80%" result) and URLEncoder's "+" for spaces doesn't round-trip.
        fun createRoute(args: GameResultArgs): String {
            fun enc(value: String) = Uri.encode(value)
            return "result/${enc(args.fileId)}/${args.gameType.name}?title=${enc(args.title)}" +
                "&big=${enc(args.big)}&caption=${enc(args.caption)}&xp=${args.xp}" +
                "&streak=${args.streak}&note=${enc(args.note)}&level=${args.level}&leveledUp=${args.leveledUp}"
        }

        fun readArgs(arguments: Bundle?): GameResultArgs = GameResultArgs(
            fileId = arguments?.getString("fileId").orEmpty(),
            gameType = runCatching { GameType.valueOf(arguments?.getString("gameType").orEmpty()) }
                .getOrDefault(GameType.QUIZ),
            title = arguments?.getString("title").orEmpty(),
            big = arguments?.getString("big").orEmpty(),
            caption = arguments?.getString("caption").orEmpty(),
            xp = arguments?.getInt("xp") ?: 0,
            streak = arguments?.getInt("streak") ?: 0,
            note = arguments?.getString("note").orEmpty(),
            level = arguments?.getInt("level") ?: 1,
            leveledUp = arguments?.getBoolean("leveledUp") ?: false
        )
    }
}

/** Route (without the game-specific segment) of the screen used to play [gameType]. */
fun gameRouteFor(gameType: GameType, fileId: String): String = when (gameType) {
    GameType.QUIZ -> Screen.Quiz.createRoute(fileId)
    GameType.FLASHCARDS -> Screen.Flashcards.createRoute(fileId)
    GameType.GUESS_WORD -> Screen.GuessWord.createRoute(fileId)
    GameType.CROSSWORD -> Screen.Crossword.createRoute(fileId)
}
