package telegram

import data.ImageHintsRepository
import domain.HintImageService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ApiResponse
import model.Question
import model.Update
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder
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

class TelegramBotService(private val token: String) {

    private val okHttpClient = OkHttpClient()

    private val json = Json { ignoreUnknownKeys = true }

    private val hintImageService = HintImageService(ImageHintsRepository(), this)

    fun getUpdates(offset: Long): List<Update> {
        val url = "${BOT_URL}$token/getUpdates?offset=$offset"
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
            response.body?.string() ?: ""
        }
    }

    fun sendQuestion(chatId: Long, question: Question): String {

        hintImageService.sendHintIfExists(question.questionWord.original, chatId)

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
            response.body?.string() ?: ""
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

    fun sendPhoto(file: File, chatId: Long, hasSpoiler: Boolean = false): String {
        val url = "${BOT_URL}$token/sendPhoto"

        val requestBody = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId.toString())
            .addFormDataPart("has_spoiler", hasSpoiler.toString())
            .addFormDataPart(
                "photo",
                file.name,
                file.readBytes().toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    fun sendPhoto(
        fileName: String,
        bytes: ByteArray,
        chatId: Long,
        hasSpoiler: Boolean = false
    ): String {
        val url = "${BOT_URL}$token/sendPhoto"

        val requestBody = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId.toString())
            .addFormDataPart("has_spoiler", hasSpoiler.toString())
            .addFormDataPart(
                "photo",
                fileName,
                bytes.toRequestBody("image/*".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean = false): String {
        val url = "${BOT_URL}$token/sendPhoto"

        val requestBody = okhttp3.FormBody.Builder()
            .add("chat_id", chatId.toString())
            .add("photo", fileId)
            .add("has_spoiler", hasSpoiler.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }
}