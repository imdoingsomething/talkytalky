package com.talkytalky.app

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/** API contract: POST /transcribe, multipart field "audio" -> {"text"} | {"error"}. No retry loop here; UI offers a manual retry. */
object TranscribeClient {
    const val TIMEOUT_MS = 20_000

    sealed class Result {
        data class Ok(val text: String) : Result()
        data class Fail(val message: String) : Result()
    }

    fun transcribe(endpoint: String, audio: File): Result {
        val boundary = "tt${System.nanoTime()}"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL("$endpoint/transcribe").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5_000
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            conn.outputStream.use { out ->
                out.write(("--$boundary\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"${audio.name}\"\r\n" +
                    "Content-Type: audio/mp4\r\n\r\n").toByteArray())
                audio.inputStream().use { it.copyTo(out) }
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val code = conn.responseCode
            val body = (if (code == 200) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
            parse(code, body)
        } catch (e: java.net.SocketTimeoutException) {
            Result.Fail("Timed out")
        } catch (e: java.io.IOException) {
            Result.Fail("Can't reach TalkyTalky server")
        } finally {
            conn?.disconnect()
        }
    }

    internal fun parse(code: Int, body: String): Result = try {
        val j = JSONObject(body)
        if (code == 200 && j.has("text")) Result.Ok(j.getString("text"))
        else Result.Fail(j.optString("error", "HTTP $code"))
    } catch (e: Exception) {
        Result.Fail("Bad response (HTTP $code)")
    }
}
