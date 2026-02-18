package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val ok: Boolean,
    val result: T? = null
)

@Serializable
data class Update(
    @SerialName("update_id") val updateId: Long,
    val message: Message? = null,
    @SerialName("callback_query") val callbackQuery: CallbackQuery? = null
)

@Serializable
data class Document(
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("file_id") val fileId: String,
    @SerialName("file_unique_id") val fileUniqueId: String,
    @SerialName("file_size") val fileSize: Long,
)

@Serializable
data class Message(
    @SerialName("message_id") val messageId: Long? = null,
    val chat: Chat,
    val text: String? = null,
    val document: Document? = null
)

@Serializable
data class Chat(
    val id: Long
)

@Serializable
data class CallbackQuery(
    val id: String? = null,
    val data: String? = null,
    val message: Message? = null
)