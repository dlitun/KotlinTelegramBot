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
data class Message(
    @SerialName("message_id") val messageId: Long? = null,
    val chat: Chat,
    val text: String? = null
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

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: InlineKeyboard? = null
)

@Serializable
data class InlineKeyboard(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboardButton>>
)

@Serializable
data class InlineKeyboardButton(
    val text: String,
    @SerialName("callback_data")
    val callbackData: String
)