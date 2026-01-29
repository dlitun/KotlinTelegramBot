package telegram

import trainer.UserTrainerManager

private const val START_COMMAND = "/start"
private const val RESET_COMMAND = "/reset"

private const val CALLBACK_LEARN = "learn_words_clicked"
private const val CALLBACK_STATS = "statistics_clicked"
private const val CALLBACK_RESET = "reset_clicked"
private const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

fun main(args: Array<String>) {
    val token = args.getOrNull(0)
        ?: error("Передай токен бота в Program arguments (Run/Debug Configuration).")

    val service = TelegramBotService(token)

    val trainerManager = UserTrainerManager(
        baseWordsFilePath = "src/main/resources/words.txt",
        usersDirPath = "users",
        minCorrect = 3
    )

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
            val text = message.text ?: ""

            when (text) {
                START_COMMAND -> {
                    service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS,    CALLBACK_RESET)
                    continue
                }

                RESET_COMMAND -> {
                    trainerManager.reset(chatId)
                    service.sendMessage(chatId, "Прогресс сброшен ✅")
                    service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS, CALLBACK_RESET)
                    continue
                }
            }
        }

        val callback = last.callbackQuery ?: continue
        val data = callback.data ?: continue
        val chatId = callback.message?.chat?.id ?: continue

        val trainer = trainerManager.getTrainer(chatId)

        when {
            data == CALLBACK_STATS -> {
                val stats = trainer.getStatistics()
                val msg = "Выучено ${stats.learnedCount} из ${stats.totalCount} слов | ${stats.percent}%"
                service.sendMessage(chatId, msg)
            }

            data == CALLBACK_LEARN -> {
                checkNextQuestionAndSend(trainerManager, service, chatId)
            }

            data == CALLBACK_RESET -> {
                trainerManager.reset(chatId)
                service.sendMessage(chatId, "Прогресс сброшен ✅")
                service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS, CALLBACK_RESET)
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
                        val w = trainer.getCurrentCorrectWord()
                        val msg = if (w != null) "Неправильно! ${w.original} – это ${w.translate}" else "Неправильно!"
                        service.sendMessage(chatId, msg)
                    }
                    checkNextQuestionAndSend(trainerManager, service, chatId)
                }
            }

            else -> service.sendMessage(chatId, "Неизвестная команда: $data")
        }
    }
}

fun checkNextQuestionAndSend(
    trainerManager: UserTrainerManager,
    telegramBotService: TelegramBotService,
    chatId: Long
) {
    val trainer = trainerManager.getTrainer(chatId)
    val question = trainer.getNextQuestion()

    if (question == null) {
        telegramBotService.sendMessage(chatId, "Все слова в словаре выучены")
        return
    }

    telegramBotService.sendQuestion(chatId, question)
}