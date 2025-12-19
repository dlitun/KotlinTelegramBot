package telegram

import trainer.LearnWordsTrainer

private const val START_COMMAND = "/start"
private const val HELLO_TEXT = "Hello"

private const val CALLBACK_LEARN = "learn_words_clicked"
private const val CALLBACK_STATS = "statistics_clicked"

fun main(args: Array<String>) {
    val token = args.getOrNull(0) ?: error("Передай токен первым аргументом: program <BOT_TOKEN>")
    val service = TelegramBotService(token)
    val trainer = LearnWordsTrainer()

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

        // 1) Callback (нажатие кнопки)
        val data = dataRegex.findAll(updates).lastOrNull()?.groupValues?.get(1)
        val callbackChatId = callbackChatIdRegex.find(updates)?.groupValues?.get(1)

        if (data != null && callbackChatId != null) {
            when (data) {
                CALLBACK_LEARN -> service.sendMessage(callbackChatId, trainer.start())
                CALLBACK_STATS -> service.sendMessage(callbackChatId, trainer.stats())
                else -> service.sendMessage(callbackChatId, "Неизвестная кнопка: $data")
            }
            continue
        }

        val text = textRegex.findAll(updates).lastOrNull()?.groupValues?.get(1)
        val messageChatId = messageChatIdRegex.findAll(updates).lastOrNull()?.groupValues?.get(1)

        if (text == START_COMMAND && messageChatId != null) {
            service.sendMenu(messageChatId, CALLBACK_LEARN, CALLBACK_STATS)
            continue
        }

        if (text == HELLO_TEXT && messageChatId != null) {
            service.sendMessage(messageChatId, HELLO_TEXT)
        }
    }
}