package br.com.studyou.data.remote

import br.com.studyou.BuildConfig
import br.com.studyou.domain.model.GeneratedGames
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * Base URL of the StudYou backend (see the FastAPI project under /backend), configured via
 * BACKEND_BASE_URL in /mobile/.env (not versioned — see .env.example) and injected at build
 * time as BuildConfig.BACKEND_BASE_URL, so it never needs to be hardcoded or recompiled by hand:
 *
 * - Physical device over USB with `adb reverse tcp:8000 tcp:8000`: http://localhost:8000
 * - Android emulator: http://10.0.2.2:8000 (the emulator's alias for the host's localhost)
 * - Device on the same Wi-Fi as the machine running the backend: http://<machine-ip>:8000
 * - A real deployment's URL for production.
 */
private val BASE_URL = BuildConfig.BACKEND_BASE_URL

sealed class FileAnalysisResult {
    data class Success(val content: String, val games: GeneratedGames) : FileAnalysisResult()
    data class Failure(val message: String) : FileAnalysisResult()
}

/** Talks to POST /api/v1/files/analyze on the StudYou backend over plain HttpURLConnection. */
class FileAnalysisApiClient {

    suspend fun analyzeFile(fileName: String, mimeType: String, content: ByteArray): FileAnalysisResult =
        withContext(Dispatchers.IO) {
            val boundary = "studyou-${UUID.randomUUID()}"
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$BASE_URL/api/v1/files/analyze").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 120_000
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                connection.outputStream.use { output ->
                    output.write(multipartHeader(boundary, fileName, mimeType))
                    output.write(content)
                    output.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
                }

                val status = connection.responseCode
                val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                if (status in 200..299) {
                    val json = JSONObject(body)
                    FileAnalysisResult.Success(
                        content = json.optString("content", body),
                        games = parseGeneratedGames(json.getJSONObject("games"))
                    )
                } else {
                    FileAnalysisResult.Failure(extractField(body, "detail") ?: "Erro inesperado do servidor ($status).")
                }
            } catch (error: SocketTimeoutException) {
                FileAnalysisResult.Failure("O servidor demorou demais para responder. Tente novamente.")
            } catch (error: ConnectException) {
                FileAnalysisResult.Failure(
                    "Não foi possível conectar ao servidor em $BASE_URL. Verifique se o backend está rodando " +
                        "e, em dispositivo físico, se o \"adb reverse tcp:8000 tcp:8000\" ainda está ativo."
                )
            } catch (error: IOException) {
                FileAnalysisResult.Failure("Falha de rede ao enviar o arquivo: ${error.message ?: "erro desconhecido"}.")
            } catch (error: JSONException) {
                FileAnalysisResult.Failure("O servidor respondeu em um formato inesperado.")
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

    private fun extractField(body: String, field: String): String? =
        try {
            JSONObject(body).optString(field).takeIf { it.isNotBlank() }
        } catch (_: JSONException) {
            null
        }
}
