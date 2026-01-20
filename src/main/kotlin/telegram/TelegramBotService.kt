package telegram

import model.Question
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.text.Charsets.UTF_8

private const val BASE_URL = "https://api.telegram.org/bot"
private const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

class TelegramBotService(private val token: String) {

    private val httpClient: HttpClient = HttpClient.newBuilder().build()
    private val okHttpClient = OkHttpClient()

    fun getUpdates(offset: Int): String {
        val url = "${BASE_URL}$token/getUpdates?offset=$offset"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.body()
        } catch (e: Exception) {
            "Ошибка getUpdates: ${e.message}"
        }
    }

    fun sendMessage(chatId: Int, text: String): String {
        val encodedText = URLEncoder.encode(text, UTF_8)
        val url = "${BASE_URL}$token/sendMessage?chat_id=$chatId&text=$encodedText"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.body()
        } catch (e: Exception) {
            "Ошибка sendMessage: ${e.message}"
        }
    }

    fun sendMenu(chatId: Int, callbackLearn: String, callbackStats: String): String {
        val url = "${BASE_URL}$token/sendMessage"

        val json = """
            {
              "chat_id": $chatId,
              "text": "Основное меню",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    { "text": "Изучить слова", "callback_data": "$callbackLearn" },
                    { "text": "Статистика", "callback_data": "$callbackStats" }
                  ]
                ]
              }
            }
        """.trimIndent()

        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            "Ошибка sendMenu: ${e.message}"
        }
    }

    fun sendQuestion(chatId: Int, question: Question): String {
        val url = "${BASE_URL}$token/sendMessage"

        val buttonsJson = question.options
            .mapIndexed { index, word ->
                """{ "text": "${word.translate}", "callback_data": "${CALLBACK_DATA_ANSWER_PREFIX}$index" }"""
            }
            .joinToString(separator = ",")

        val json = """
            {
              "chat_id": $chatId,
              "text": "${question.questionWord.original}",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    $buttonsJson
                  ]
                ]
              }
            }
        """.trimIndent()

        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            "Ошибка sendQuestion: ${e.message}"
        }
    }
}