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

    var offset = 0L

    while (true) {
        Thread.sleep(2000)

        val updates = service.getUpdates(offset)
        if (updates.isEmpty()) continue

        offset = updates.maxOf { it.updateId } + 1

        val last = updates.last()

        val message = last.message
        if (message != null) {
            val chatId = message.chat.id
            val text = message.text

            if (text == START_COMMAND) {
                service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS)
                continue
            }

            if (text == HELLO_TEXT) {
                service.sendMessage(chatId, HELLO_TEXT)
                continue
            }
        }

        val callback = last.callbackQuery
        if (callback != null) {
            val data = callback.data ?: continue
            val chatId = callback.message?.chat?.id ?: continue

            when {
                data == CALLBACK_STATS -> {
                    val stats = trainer.getStatistics()
                    val msg = "Выучено ${stats.learnedCount} из ${stats.totalCount} слов | ${stats.percent}%"
                    service.sendMessage(chatId, msg)
                }

                data == CALLBACK_LEARN -> {
                    checkNextQuestionAndSend(trainer, service, chatId)
                }

                data.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                    val idx = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()

                    if (idx == null) {
                        service.sendMessage(chatId, "Некорректный ответ: $data")
                    } else {
                        val isCorrect = trainer.checkAnswer(idx)

                        if (isCorrect) {
                            service.sendMessage(chatId, "Правильно!")
                        } else {
                            service.sendMessage(chatId, "Неправильно!")
                        }

                        checkNextQuestionAndSend(trainer, service, chatId)
                    }
                }

                else -> service.sendMessage(chatId, "Неизвестная команда: $data")
            }
        }
    }
}

fun checkNextQuestionAndSend(
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Long
) {
    val question: Question? = trainer.getNextQuestion()

    if (question == null) {
        telegramBotService.sendMessage(chatId, "Все слова в словаре выучены")
        return
    }

    telegramBotService.sendQuestion(chatId, question)
}