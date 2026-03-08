package telegram

class DynamicMessage {
    private val messageIdsByChat = mutableMapOf<Long, Long>()
    private val messageHistoryByChat = mutableMapOf<Long, ArrayDeque<String>>()

    fun setMessageId(chatId: Long, messageId: Long) {
        messageIdsByChat[chatId] = messageId
    }

    fun getMessageId(chatId: Long): Long? = messageIdsByChat[chatId]

    fun hasMessage(chatId: Long): Boolean = messageIdsByChat.containsKey(chatId)

    fun rememberText(chatId: Long, text: String) {
        val history = messageHistoryByChat.getOrPut(chatId) { ArrayDeque() }
        if (history.lastOrNull() == text) return
        history.addLast(text)
    }

    fun rollbackText(chatId: Long): String? {
        val history = messageHistoryByChat[chatId] ?: return null
        if (history.size < 2) return history.lastOrNull()

        history.removeLastOrNull()
        return history.lastOrNull()
    }

    fun clear(chatId: Long) {
        messageIdsByChat.remove(chatId)
        messageHistoryByChat.remove(chatId)
    }
}

