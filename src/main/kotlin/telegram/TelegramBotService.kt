package telegram

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ApiResponse
import model.Update
import model.Question
import model.SendMessageRequest
import model.InlineKeyboard
import model.InlineKeyboardButton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val BASE_URL = "https://api.telegram.org/bot"
private const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

class TelegramBotService(private val token: String) {

    private val okHttpClient = OkHttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getUpdates(offset: Long): List<Update> {
        val url = "${BASE_URL}$token/getUpdates?offset=$offset"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val bodyString = okHttpClient.newCall(request).execute().use { response ->
            response.body?.string().orEmpty()
        }

        val serializer = ApiResponse.serializer(ListSerializer(Update.serializer()))
        val apiResponse = json.decodeFromString(serializer, bodyString)

        return apiResponse.result ?: emptyList()
    }

    fun sendMessage(chatId: Long, text: String): String {
        return postSendMessage(
            SendMessageRequest(
                chatId = chatId,
                text = text
            )
        )
    }

    fun sendMenu(chatId: Long, callbackLearn: String, callbackStats: String): String {
        val markup = InlineKeyboard(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "Изучить слова", callbackData = callbackLearn),
                    InlineKeyboardButton(text = "Статистика", callbackData = callbackStats)
                )
            )
        )

        return postSendMessage(
            SendMessageRequest(
                chatId = chatId,
                text = "Основное меню",
                replyMarkup = markup
            )
        )
    }

    fun sendQuestion(chatId: Long, question: Question): String {
        val keyboardRows = question.options.mapIndexed { index, option ->
            listOf(
                InlineKeyboardButton(
                    text = option.translate,
                    callbackData = CALLBACK_DATA_ANSWER_PREFIX + index
                )
            )
        }

        val markup = InlineKeyboard(inlineKeyboard = keyboardRows)

        return postSendMessage(
            SendMessageRequest(
                chatId = chatId,
                text = question.questionWord.original,
                replyMarkup = markup
            )
        )
    }

    private fun postSendMessage(requestModel: SendMessageRequest): String {
        val url = "${BASE_URL}$token/sendMessage"

        val jsonBody = json.encodeToString(requestModel)
        val body = jsonBody.toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            response.body?.string().orEmpty()
        }
    }
}