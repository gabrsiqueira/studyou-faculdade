package br.com.studyou.data.remote

import br.com.studyou.domain.model.CrosswordDirection
import br.com.studyou.domain.model.CrosswordEntry
import br.com.studyou.domain.model.CrosswordPuzzle
import br.com.studyou.domain.model.Flashcard
import br.com.studyou.domain.model.GeneratedGames
import br.com.studyou.domain.model.GuessWordPuzzle
import br.com.studyou.domain.model.QuizQuestion
import br.com.studyou.domain.model.WordFormingPuzzle
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Parses the `games` object inside the backend's /files/analyze JSON response. */
fun parseGeneratedGames(games: JSONObject): GeneratedGames {
    return GeneratedGames(
        quiz = parseQuiz(games.getJSONArray("quiz")),
        flashcards = parseFlashcards(games.getJSONArray("flashcards")),
        guessWord = parseGuessWord(games.getJSONObject("guess_word")),
        crossword = parseCrossword(games.getJSONObject("crossword")),
        wordForming = parseWordForming(games.getJSONObject("word_forming"))
    )
}

private fun parseQuiz(array: JSONArray): List<QuizQuestion> = (0 until array.length()).map { i ->
    val item = array.getJSONObject(i)
    QuizQuestion(
        id = UUID.randomUUID().toString(),
        prompt = item.getString("prompt"),
        options = item.getJSONArray("options").toStringList(),
        correctOptionIndex = item.getInt("correct_option_index")
    )
}

private fun parseFlashcards(array: JSONArray): List<Flashcard> = (0 until array.length()).map { i ->
    val item = array.getJSONObject(i)
    Flashcard(id = UUID.randomUUID().toString(), question = item.getString("question"), answer = item.getString("answer"))
}

private fun parseGuessWord(obj: JSONObject): GuessWordPuzzle = GuessWordPuzzle(
    title = "",
    answer = obj.getString("answer"),
    maxAttempts = obj.getInt("max_attempts")
)

private fun parseCrossword(obj: JSONObject): CrosswordPuzzle {
    val entries = obj.getJSONArray("entries")
    return CrosswordPuzzle(
        title = "",
        size = obj.getInt("size"),
        letters = obj.getJSONArray("letters").toStringList().mapNotNull { it.firstOrNull() },
        entries = (0 until entries.length()).map { i ->
            val item = entries.getJSONObject(i)
            CrosswordEntry(
                direction = if (item.getString("direction") == "ACROSS") {
                    CrosswordDirection.ACROSS
                } else {
                    CrosswordDirection.DOWN
                },
                answer = item.getString("answer"),
                row = item.getInt("row"),
                col = item.getInt("col")
            )
        },
        bonusWords = obj.getJSONArray("bonus_words").toStringList().toSet()
    )
}

private fun parseWordForming(obj: JSONObject): WordFormingPuzzle = WordFormingPuzzle(
    title = "",
    letters = obj.getJSONArray("letters").toStringList().mapNotNull { it.firstOrNull() },
    validWords = obj.getJSONArray("valid_words").toStringList().toSet()
)

private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }
