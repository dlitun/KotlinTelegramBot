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

private const val BOT_TOKEN = "8591825097:AAF0jbqDCJV1xfHkA-ZFp-O1XyF1kNz2GcM"

fun main() {
    val service = TelegramBotService(BOT_TOKEN)

    var offset = 0

    val updateIdRegex = "\"update_id\"\\s*:\\s*(\\d+)".toRegex()
    val textRegex = "\"text\"\\s*:\\s*\"(.+?)\"".toRegex()
    val messageChatIdRegex = "\"chat\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*(-?\\d+)".toRegex()

    val dataRegex = "\"data\"\\s*:\\s*\"(.+?)\"".toRegex()
    val callbackChatIdRegex =
        "\"callback_query\"\\s*:\\s*\\{.*?\"message\"\\s*:\\s*\\{.*?\"chat\"\\s*:\\s*\\{.*?\"id\"\\s*:\\s*(-?\\d+)".toRegex(
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

    while (true) {
        Thread.sleep(2000)

        val updates = service.getUpdates(offset)
        println(updates)

        val lastUpdateMatch = updateIdRegex.findAll(updates).lastOrNull() ?: continue
        val lastUpdateId = lastUpdateMatch.groupValues[1].toInt()
        offset = lastUpdateId + 1

        val text = textRegex.findAll(updates).lastOrNull()?.groupValues?.get(1)
        val messageChatId = messageChatIdRegex.findAll(updates).lastOrNull()?.groupValues?.get(1)

        if (text == "/start" && messageChatId != null) {
            service.sendMenu(messageChatId)
            continue
        }

        if (text == "Hello" && messageChatId != null) {
            service.sendMessage(messageChatId, "Hello")
        }

        val data = dataRegex.findAll(updates).lastOrNull()?.groupValues?.get(1)
        val callbackChatId = callbackChatIdRegex.find(updates)?.groupValues?.get(1)

        if (data != null && callbackChatId != null) {
            when (data) {
                "learn_words_clicked" -> service.sendMessage(callbackChatId, "Вы выбрали: Изучить слова")
                "statistics_clicked" -> service.sendMessage(callbackChatId, "Выучено 0 из 0 слов | 0%")
                else -> service.sendMessage(callbackChatId, "Неизвестная кнопка: $data")
            }
        }
    }
}

class TelegramBotService(private val token: String) {

    private val httpClient: HttpClient = HttpClient.newBuilder().build()
    private val okHttpClient = OkHttpClient()

    fun getUpdates(offset: Int): String {
        val url = "https://api.telegram.org/bot$token/getUpdates?offset=$offset"

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

    fun sendMessage(chatId: String, text: String): String {
        val encodedText = URLEncoder.encode(text, UTF_8)
        val url = "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$encodedText"

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

    fun sendMenu(chatId: String): String {
        val url = "https://api.telegram.org/bot$token/sendMessage"

        val json = """
            {
              "chat_id": $chatId,
              "text": "Основное меню",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    { "text": "Изучить слова", "callback_data": "learn_words_clicked" },
                    { "text": "Статистика", "callback_data": "statistics_clicked" }
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
}