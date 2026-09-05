package br.com.studyou.data.remote

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
 * Base URL of the StudYou backend (see the FastAPI project under /backend).
 *
 * Set for testing on a physical device over USB: with the backend running on your machine
 * (`docker compose up` in /backend), run `adb reverse tcp:8000 tcp:8000` so the device's
 * "localhost:8000" is forwarded to your machine's "localhost:8000" — then this just works.
 *
 * Testing on the Android *emulator* instead? Use "http://10.0.2.2:8000" — the emulator's
 * special alias for the host machine's localhost — and skip the adb reverse step.
 * Point this at a real deployment's URL for production.
 */
private const val BASE_URL = "http://localhost:8000"

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
