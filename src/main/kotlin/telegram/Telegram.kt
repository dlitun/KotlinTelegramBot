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
        baseWordsFilePath = "words.txt",
        usersDirPath = "users",
        minCorrect = 3
    )

    var offset = 0L

    while (true) {
        try {
            // можно реже спать, т.к. у тебя long polling в getUpdates (timeout=50)
            // но оставим маленькую паузу, чтобы не долбить в случае ошибок
            Thread.sleep(300)

            val updates = service.getUpdates(offset)
            if (updates.isEmpty()) continue

            // ВАЖНО: сначала обновляем offset по максимуму
            offset = updates.maxOf { it.updateId } + 1

            // ВАЖНО: обрабатываем КАЖДЫЙ апдейт, а не только последний
            for (u in updates) {

                // 1) Текстовые сообщения (/start, /reset)
                u.message?.let { message ->
                    val chatId = message.chat.id
                    val text = message.text ?: ""

                    when (text) {
                        START_COMMAND -> {
                            service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS, CALLBACK_RESET)
                        }

                        RESET_COMMAND -> {
                            trainerManager.reset(chatId)
                            service.sendMessage(chatId, "Прогресс сброшен ✅")
                            service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS, CALLBACK_RESET)
                        }

                        else -> {
                            // если хочешь — можешь отвечать на любые тексты
                            // service.sendMessage(chatId, "Нажми кнопку в меню 👇")
                        }
                    }
                }

                // 2) Callback-кнопки
                val callback = u.callbackQuery ?: continue
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
                        val answerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()

                        if (answerIndex == null) {
                            service.sendMessage(chatId, "Некорректный ответ: $data")
                        } else {
                            val isCorrect = trainer.checkAnswer(answerIndex)

                            if (isCorrect) {
                                service.sendMessage(chatId, "Правильно!")
                            } else {
                                val word = trainer.getCurrentCorrectWord()
                                val msg =
                                    if (word != null) "Неправильно! ${word.original} — это ${word.translate}"
                                    else "Неправильно!"
                                service.sendMessage(chatId, msg)
                            }

                            checkNextQuestionAndSend(trainerManager, service, chatId)
                        }
                    }

                    else -> service.sendMessage(chatId, "Неизвестная команда: $data")
                }
            }

        } catch (e: Exception) {
            println("Main loop error: ${e.message}")
            Thread.sleep(3000)
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