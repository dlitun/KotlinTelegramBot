package telegram

import model.Question
import trainer.LearnWordsTrainer

private const val START_COMMAND = "/start"
private const val HELLO_TEXT = "Hello"

private const val CALLBACK_LEARN = "learn_words_clicked"
private const val CALLBACK_STATS = "statistics_clicked"

private const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

fun main(args: Array<String>) {
    val token = args.getOrNull(0)
        ?: error("Передай токен бота в Program arguments (Run/Debug Configuration).")

    val service = TelegramBotService(token)
    val trainer = LearnWordsTrainer()

    var offset = 0

    val updateIdRegex = "\"update_id\"\\s*:\\s*(\\d+)".toRegex()
    val textRegex = "\"text\"\\s*:\\s*\"(.+?)\"".toRegex()
    val messageChatIdRegex = "\"chat\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*(-?\\d+)".toRegex()

    val dataRegex = "\"data\"\\s*:\\s*\"(.+?)\"".toRegex()
    val callbackChatIdRegex =
        "\"callback_query\"\\s*:\\s*\\{.*?\"message\"\\s*:\\s*\\{.*?\"chat\"\\s*:\\s*\\{.*?\"id\"\\s*:\\s*(-?\\d+)"
            .toRegex(setOf(RegexOption.DOT_MATCHES_ALL))

    while (true) {
        Thread.sleep(2000)

        val updates = service.getUpdates(offset)
        println(updates)

        val lastUpdateMatch = updateIdRegex.findAll(updates).lastOrNull() ?: continue
        offset = lastUpdateMatch.groupValues[1].toInt() + 1

        val text = textRegex.findAll(updates).lastOrNull()?.groupValues?.get(1)
        val messageChatId = messageChatIdRegex.findAll(updates).lastOrNull()?.groupValues?.get(1)?.toInt()

        if (text == START_COMMAND && messageChatId != null) {
            service.sendMenu(messageChatId, CALLBACK_LEARN, CALLBACK_STATS)
            continue
        }

        if (text == HELLO_TEXT && messageChatId != null) {
            service.sendMessage(messageChatId, HELLO_TEXT)
        }

        val data = dataRegex.findAll(updates).lastOrNull()?.groupValues?.get(1)
        val callbackChatId = callbackChatIdRegex.find(updates)?.groupValues?.get(1)?.toInt()

        if (data != null && callbackChatId != null) {
            when {
                data == CALLBACK_STATS -> {
                    val stats = trainer.getStatistics()
                    val message = "Выучено ${stats.learnedCount} из ${stats.totalCount} слов | ${stats.percent}%"
                    service.sendMessage(callbackChatId, message)
                }

                data == CALLBACK_LEARN -> {
                    checkNextQuestionAndSend(trainer, service, callbackChatId)
                }

                data.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                    val indexString = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX)
                    val userAnswerIndex = indexString.toIntOrNull()

                    if (userAnswerIndex == null) {
                        service.sendMessage(callbackChatId, "Некорректный ответ: $data")
                    } else {
                        val isCorrect = trainer.checkAnswer(userAnswerIndex)

                        if (isCorrect) {
                            service.sendMessage(callbackChatId, "Правильно!")
                        } else {
                            val correctWord = trainer.getCorrectWordForCurrentQuestion()
                            if (correctWord != null) {
                                service.sendMessage(
                                    callbackChatId,
                                    "Неправильно! ${correctWord.original} – это ${correctWord.translate}"
                                )
                            } else {
                                service.sendMessage(callbackChatId, "Неправильно!")
                            }
                        }

                        checkNextQuestionAndSend(trainer, service, callbackChatId)
                    }
                }

                else -> {
                    service.sendMessage(callbackChatId, "Неизвестная команда: $data")
                }
            }
        }
    }
}

fun checkNextQuestionAndSend(
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Int
) {
    val question: Question? = trainer.getNextQuestion()

    if (question == null) {
        telegramBotService.sendMessage(chatId, "Все слова в словаре выучены")
        return
    }

    telegramBotService.sendQuestion(chatId, question)
}