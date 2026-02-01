package telegram

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import model.ApiResponse
import model.Question
import model.Update
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

private const val BASE_URL = "https://api.telegram.org/bot"
private const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

class TelegramBotService(private val token: String) {

    private val okHttpClient = OkHttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getUpdates(offset: Long): List<Update> {
        val url = "${BASE_URL}$token/getUpdates?offset=$offset"
        val request = Request.Builder().url(url).get().build()

        val bodyString = okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }

        val apiResponse = json.decodeFromString(
            ApiResponse.serializer(ListSerializer(Update.serializer())),
            bodyString
        )

        return apiResponse.result ?: emptyList()
    }

    fun sendMessage(chatId: Long, text: String): String {
        val encodedText = URLEncoder.encode(text, UTF_8)
        val url = "${BASE_URL}$token/sendMessage?chat_id=$chatId&text=$encodedText"
        val request = Request.Builder().url(url).get().build()

        return okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    fun sendMenu(chatId: Long, callbackLearn: String, callbackStats: String, callbackReset: String): String {
        val url = "${BASE_URL}$token/sendMessage"

        val jsonBody = """
            {
              "chat_id": $chatId,
              "text": "Основное меню",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    { "text": "Изучить слова", "callback_data": "$callbackLearn" },
                    { "text": "Статистика", "callback_data": "$callbackStats" },
                    { "text": "Сброс", "callback_data": "$callbackReset" }
                  ]
                ]
              }
            }
        """.trimIndent()

        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        return okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    fun sendQuestion(chatId: Long, question: Question): String {
        val url = "${BASE_URL}$token/sendMessage"

        val buttonsJson = question.options
            .mapIndexed { index, option ->
                """{ "text": "${option.translate}", "callback_data": "${CALLBACK_DATA_ANSWER_PREFIX}$index" }"""
            }
            .joinToString(",")

        val jsonBody = """
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

        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        return okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }
}