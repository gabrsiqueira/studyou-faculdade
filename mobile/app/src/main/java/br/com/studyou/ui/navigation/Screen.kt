package br.com.studyou.ui.navigation

import br.com.studyou.domain.model.GameType
import java.net.URLEncoder

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

    object WordForming : Screen("word-forming/{fileId}") {
        fun createRoute(fileId: String) = "word-forming/$fileId"
    }

    /** Generic result screen, fed with everything it needs to render via query parameters. */
    object Result : Screen(
        "result/{fileId}/{gameType}?title={title}&big={big}&caption={caption}" +
            "&xp={xp}&streak={streak}&note={note}"
    ) {
        fun createRoute(
            fileId: String,
            gameType: GameType,
            title: String,
            big: String,
            caption: String,
            xp: Int,
            streak: Int,
            note: String
        ): String {
            fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
            return "result/$fileId/${gameType.name}?title=${enc(title)}&big=${enc(big)}" +
                "&caption=${enc(caption)}&xp=$xp&streak=$streak&note=${enc(note)}"
        }
    }
}

/** Route (without the game-specific segment) of the screen used to play [gameType]. */
fun gameRouteFor(gameType: GameType, fileId: String): String = when (gameType) {
    GameType.QUIZ -> Screen.Quiz.createRoute(fileId)
    GameType.FLASHCARDS -> Screen.Flashcards.createRoute(fileId)
    GameType.GUESS_WORD -> Screen.GuessWord.createRoute(fileId)
    GameType.CROSSWORD -> Screen.Crossword.createRoute(fileId)
    GameType.WORD_FORMING -> Screen.WordForming.createRoute(fileId)
}
