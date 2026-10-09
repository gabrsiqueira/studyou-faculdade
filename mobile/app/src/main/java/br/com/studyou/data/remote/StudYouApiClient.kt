package br.com.studyou.data.remote

import br.com.studyou.domain.model.AuthUser
import br.com.studyou.domain.model.GameAttempt
import br.com.studyou.domain.model.GameType
import br.com.studyou.domain.model.GeneratedGames
import br.com.studyou.domain.model.NewGameAttempt
import br.com.studyou.domain.model.Session
import java.io.IOException
import java.io.OutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()
}

/** A file the backend has already analyzed, together with the games generated from it. */
data class AnalyzedFile(val id: String, val fileName: String, val games: GeneratedGames)

/**
 * Talks to the StudYou backend (see the FastAPI project under /backend) over plain
 * HttpURLConnection. Authenticated calls send the current session's token; if the backend
 * rejects it (expired or revoked), [onUnauthorized] is called so the app can log the user out.
 *
 * [baseUrl] comes from BACKEND_BASE_URL in /mobile/.env (see .env.example).
 */
class StudYouApiClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    private val onUnauthorized: () -> Unit
) {

    suspend fun register(name: String, email: String, password: String): ApiResult<Session> =
        postJson(
            path = "/api/v1/auth/register",
            body = JSONObject().put("name", name).put("email", email).put("password", password),
            authenticated = false
        ) { parseSession(JSONObject(it)) }

    suspend fun login(email: String, password: String): ApiResult<Session> =
        postJson(
            path = "/api/v1/auth/login",
            body = JSONObject().put("email", email).put("password", password),
            authenticated = false
        ) { parseSession(JSONObject(it)) }

    suspend fun analyzeFile(fileName: String, mimeType: String, content: ByteArray): ApiResult<AnalyzedFile> {
        val boundary = "studyou-${UUID.randomUUID()}"
        return execute(
            method = "POST",
            path = "/api/v1/files/analyze",
            authenticated = true,
            // Analyzing the file and generating every game are two AI calls that can take a
            // few minutes for large files, so this request waits much longer than the others.
            readTimeoutMillis = ANALYZE_READ_TIMEOUT_MILLIS,
            contentType = "multipart/form-data; boundary=$boundary",
            writeBody = { output ->
                output.write(multipartHeader(boundary, fileName, mimeType))
                output.write(content)
                output.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
            }
        ) { parseAnalyzedFile(JSONObject(it)) }
    }

    suspend fun listFiles(): ApiResult<List<AnalyzedFile>> =
        execute(method = "GET", path = "/api/v1/files", authenticated = true) { body ->
            JSONArray(body).let { array -> (0 until array.length()).map { parseAnalyzedFile(array.getJSONObject(it)) } }
        }

    suspend fun listAttempts(): ApiResult<List<GameAttempt>> =
        execute(method = "GET", path = "/api/v1/history/attempts", authenticated = true) { body ->
            // Attempts of games that no longer exist in the app (e.g. the removed Forma-Palavras) are skipped.
            JSONArray(body).let { array ->
                (0 until array.length()).mapNotNull { index ->
                    val json = array.getJSONObject(index)
                    gameTypeOf(json)?.let { parseAttempt(json, it) }
                }
            }
        }

    suspend fun saveAttempt(attempt: NewGameAttempt): ApiResult<GameAttempt> =
        postJson(
            path = "/api/v1/history/attempts",
            body = JSONObject()
                .put("file_id", attempt.fileId)
                .put("game_type", attempt.gameType.name)
                .put("file_title", attempt.fileTitle)
                .put("score_label", attempt.scoreLabel)
                .put("score_percent", attempt.scorePercent.coerceIn(0, 100))
                .put("xp_earned", attempt.xpEarned)
                .put("best_score_label", attempt.bestScoreLabel),
            authenticated = true
        ) { parseAttempt(JSONObject(it), attempt.gameType) }

    private suspend fun <T> postJson(
        path: String,
        body: JSONObject,
        authenticated: Boolean,
        parse: (String) -> T
    ): ApiResult<T> = execute(
        method = "POST",
        path = path,
        authenticated = authenticated,
        contentType = "application/json; charset=utf-8",
        writeBody = { it.write(body.toString().toByteArray(Charsets.UTF_8)) },
        parse = parse
    )

    private suspend fun <T> execute(
        method: String,
        path: String,
        authenticated: Boolean,
        readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
        contentType: String? = null,
        writeBody: ((OutputStream) -> Unit)? = null,
        parse: (String) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = readTimeoutMillis
                setRequestProperty("Accept", "application/json")
                contentType?.let { setRequestProperty("Content-Type", it) }
                if (authenticated) {
                    tokenProvider()?.let { setRequestProperty("Authorization", "Bearer $it") }
                }
                doOutput = writeBody != null
            }
            writeBody?.let { write -> connection.outputStream.use(write) }

            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            when {
                status in 200..299 -> ApiResult.Success(parse(body))
                status == HttpURLConnection.HTTP_UNAUTHORIZED && authenticated -> {
                    onUnauthorized()
                    ApiResult.Failure("Sua sessão expirou. Entre novamente.")
                }
                else -> ApiResult.Failure(errorDetail(body) ?: "Erro inesperado do servidor ($status).")
            }
        } catch (error: SocketTimeoutException) {
            ApiResult.Failure("O servidor demorou demais para responder. Tente novamente.")
        } catch (error: ConnectException) {
            ApiResult.Failure(
                "Não foi possível conectar ao servidor em $baseUrl. Verifique se o backend está rodando " +
                    "e se o celular está na mesma rede."
            )
        } catch (error: IOException) {
            ApiResult.Failure("Falha de rede: ${error.message ?: "erro desconhecido"}.")
        } catch (error: JSONException) {
            ApiResult.Failure("O servidor respondeu em um formato inesperado.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun multipartHeader(boundary: String, fileName: String, mimeType: String): ByteArray =
        buildString {
            append("--$boundary\r\n")
            append("Content-Disposition: form-data; name=\"file\"; filename=\"${fileName.replace("\"", "")}\"\r\n")
            append("Content-Type: ${mimeType.ifBlank { "application/octet-stream" }}\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)

    /** FastAPI returns `detail` as a string for our errors, or a list for validation errors. */
    private fun errorDetail(body: String): String? = try {
        when (val detail = JSONObject(body).opt("detail")) {
            is String -> detail.takeIf { it.isNotBlank() }
            is JSONArray -> detail.optJSONObject(0)?.optString("msg")?.removePrefix("Value error, ")?.takeIf { it.isNotBlank() }
            else -> null
        }
    } catch (_: JSONException) {
        null
    }

    private fun parseSession(json: JSONObject): Session {
        val user = json.getJSONObject("user")
        return Session(
            token = json.getString("token"),
            user = AuthUser(id = user.getString("id"), name = user.getString("name"), email = user.getString("email"))
        )
    }

    private fun parseAnalyzedFile(json: JSONObject): AnalyzedFile = AnalyzedFile(
        id = json.getString("id"),
        fileName = json.getString("filename"),
        games = parseGeneratedGames(json.getJSONObject("games"))
    )

    private fun gameTypeOf(json: JSONObject): GameType? =
        GameType.entries.firstOrNull { it.name == json.getString("game_type") }

    private fun parseAttempt(json: JSONObject, gameType: GameType): GameAttempt = GameAttempt(
        id = json.getString("id"),
        fileId = json.optString("file_id").takeIf { it.isNotBlank() && it != "null" },
        gameType = gameType,
        fileTitle = json.getString("file_title"),
        playedAtMillis = parseIsoInstantMillis(json.getString("played_at")),
        scoreLabel = json.getString("score_label"),
        scorePercent = json.optInt("score_percent", 0),
        xpEarned = json.getInt("xp_earned"),
        bestScoreLabel = json.getString("best_score_label")
    )

    private companion object {
        const val DEFAULT_READ_TIMEOUT_MILLIS = 120_000
        const val ANALYZE_READ_TIMEOUT_MILLIS = 300_000
    }
}
