package telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ApiResponse
import model.Question
import model.Update
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets.UTF_8

private const val BOT_URL = "https://api.telegram.org/bot"
private const val BOT_FILE_URL = "https://api.telegram.org/file/bot"
private const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

@Serializable
data class GetFileRequest(
    @SerialName("file_id") val fileId: String
)

@Serializable
data class GetFileResponse(
    val ok: Boolean,
    val result: TelegramFile? = null
)

@Serializable
data class TelegramFile(
    @SerialName("file_id") val fileId: String,
    @SerialName("file_unique_id") val fileUniqueId: String,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("file_path") val filePath: String
)

@Serializable
private data class SendPhotoResponse(
    val ok: Boolean,
    val result: SendPhotoMessage? = null,
    val description: String? = null
)

@Serializable
private data class SendPhotoMessage(
    val photo: List<PhotoSize> = emptyList()
)

@Serializable
private data class PhotoSize(
    @SerialName("file_id") val fileId: String,
    @SerialName("file_size") val fileSize: Int? = null
)

class TelegramBotService(private val token: String) {

    private val okHttpClient = OkHttpClient.Builder()
        // На VPS иногда залипает HTTP/2 стрим и валится таймаутами.
        // Принудительно используем HTTP/1.1 + явные таймауты.
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun getUpdates(offset: Long): List<Update> {
        val url = "${BOT_URL}$token/getUpdates?offset=$offset"
        val request = Request.Builder().url(url).get().build()

        val bodyString = okHttpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                println("DEBUG: getUpdates http=${response.code} body=$respBody")
            }
            respBody
        }

        val apiResponse = json.decodeFromString(
            ApiResponse.serializer(ListSerializer(Update.serializer())),
            bodyString
        )
        return apiResponse.result ?: emptyList()
    }

    fun sendMessage(chatId: Long, text: String): String {
        val encodedText = URLEncoder.encode(text, UTF_8)
        val url = "${BOT_URL}$token/sendMessage?chat_id=$chatId&text=$encodedText"
        val request = Request.Builder().url(url).get().build()

        return okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    fun sendMenu(
        chatId: Long,
        callbackLearn: String,
        callbackStats: String,
        callbackReset: String
    ): String {
        val url = "${BOT_URL}$token/sendMessage"

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
            val respBody = response.body?.string() ?: ""
            println("DEBUG: sendMenu http=${response.code} body=$respBody")
            respBody
        }
    }

    fun sendQuestion(chatId: Long, question: Question): String {
        val url = "${BOT_URL}$token/sendMessage"

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
            val respBody = response.body?.string() ?: ""
            println("DEBUG: sendQuestion http=${response.code} body=$respBody")
            respBody
        }
    }

    fun sendPhotoByFileId(chatId: Long, fileId: String, hasSpoiler: Boolean = false, caption: String? = null): String {
        val url = "${BOT_URL}$token/sendPhoto"

        val form = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId.toString())
            .addFormDataPart("photo", fileId)
            .addFormDataPart("has_spoiler", hasSpoiler.toString())

        if (!caption.isNullOrBlank()) {
            form.addFormDataPart("caption", caption)
        }

        val request = Request.Builder()
            .url(url)
            .post(form.build())
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string() ?: ""
            println("DEBUG: sendPhoto(file_id) http=${response.code} body=$respBody")
            respBody
        }
    }

    fun sendPhotoByFile(chatId: Long, file: File, hasSpoiler: Boolean = false, caption: String? = null): String {
        require(file.exists()) { "Photo file not found: ${file.absolutePath}" }

        val url = "${BOT_URL}$token/sendPhoto"

        val form = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId.toString())
            .addFormDataPart("has_spoiler", hasSpoiler.toString())
            .addFormDataPart(
                "photo",
                file.name,
                file.asRequestBody(guessImageMimeType(file.name).toMediaType())
            )

        if (!caption.isNullOrBlank()) {
            form.addFormDataPart("caption", caption)
        }

        val request = Request.Builder()
            .url(url)
            .post(form.build())
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string() ?: ""
            println("DEBUG: sendPhoto(file) http=${response.code} body=$respBody")
            respBody
        }
    }

    fun extractBestPhotoFileId(sendPhotoResponseJson: String): String? {
        return try {
            val parsed = json.decodeFromString(SendPhotoResponse.serializer(), sendPhotoResponseJson)
            if (!parsed.ok) return null
            val best = parsed.result?.photo?.maxByOrNull { it.fileSize ?: 0 } ?: return null
            best.fileId
        } catch (_: Exception) {
            null
        }
    }

    private fun guessImageMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    fun getFilePath(fileId: String): String? {
        val url = "${BOT_URL}$token/getFile"

        val requestBody = json.encodeToString(GetFileRequest(fileId))
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val bodyString = okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }

        val responseObj = json.decodeFromString(GetFileResponse.serializer(), bodyString)
        return responseObj.result?.filePath
    }

    fun downloadFile(filePath: String, saveAs: String) {
        val url = "${BOT_FILE_URL}$token/$filePath"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("downloadFile failed: ${response.code}")
            val bytes = response.body?.bytes() ?: error("empty file body")
            File(saveAs).writeBytes(bytes)
        }
    }

    fun answerCallbackQuery(callbackQueryId: String) {
        val url = "${BOT_URL}$token/answerCallbackQuery"

        val jsonBody = """
            {
              "callback_query_id": "$callbackQueryId"
            }
        """.trimIndent()

        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        okHttpClient.newCall(request).execute().use { /* просто закрываем */ }
    }
}