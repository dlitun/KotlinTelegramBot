package telegram

import model.Statistics
import security.InputSecurity
import trainer.UserTrainerManager
import java.io.File
import java.nio.charset.Charset

private const val START_COMMAND = "/start"
private const val RESET_COMMAND = "/reset"
private const val UNDO_COMMAND = "/undo"

private const val CALLBACK_LEARN = "learn_words_clicked"
private const val CALLBACK_STATS = "statistics_clicked"
private const val CALLBACK_RESET = "reset_clicked"
private const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
private val callbackAnswerRegex = Regex("^${Regex.escape(CALLBACK_DATA_ANSWER_PREFIX)}\\d+$")
private val allowedCallbackValues = setOf(CALLBACK_LEARN, CALLBACK_STATS, CALLBACK_RESET)

fun main(args: Array<String>) {
    val token = args.getOrNull(0)
        ?: error("Передай токен бота в Program arguments (Run/Debug Configuration).")

    val service = TelegramBotService(token)

    val trainerManager = UserTrainerManager(
        baseWordsFilePath = "src/main/resources/words.txt",
        dbFilePath = "trainer.db",
        minCorrect = 3
    )

    val dynamicMessage = DynamicMessage()
    val animationFrameByChat = mutableMapOf<Long, Int>()

    // Картинки-подсказки: индекс из resources, кэш fileId рядом с jar/в папке запуска
    val imageIndex = ImageIndex.loadFromResources("image_index.json")

    val fileIdCache = ImageFileIdCache(File("image_fileids.properties"))

    var offset = 0L

    while (true) {
        try {
            Thread.sleep(2000)

            val updates = service.getUpdates(offset)
            if (updates.isEmpty()) continue
            offset = updates.maxOf { it.updateId } + 1

            for (update in updates) {

                update.message?.let { message ->
                    val chatId = message.chat.id

                    val document = message.document
                    if (document != null) {
                        handleDictionaryUpload(
                            chatId = chatId,
                            documentFileId = document.fileId,
                            documentUniqueId = document.fileUniqueId,
                            originalFileName = document.fileName,
                            service = service,
                            trainerManager = trainerManager
                        )
                        return@let
                    }

                    val text = message.text ?: return@let
                    if (InputSecurity.containsSuspiciousSqlPattern(text)) {
                        InputSecurity.logSuspiciousInput("message.text", text, chatId)
                    }

                    when (text) {
                        START_COMMAND -> {
                            service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS, CALLBACK_RESET)
                        }

                        RESET_COMMAND -> {
                            trainerManager.reset(chatId)
                            dynamicMessage.clear(chatId)
                            animationFrameByChat.remove(chatId)
                            service.sendMessage(chatId, "Прогресс сброшен ✅")
                            service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS, CALLBACK_RESET)
                        }

                        UNDO_COMMAND -> {
                            handleUndoCommand(
                                chatId = chatId,
                                trainerManager = trainerManager,
                                telegramBotService = service,
                                dynamicMessage = dynamicMessage,
                                animationFrameByChat = animationFrameByChat
                            )
                        }
                    }
                }

                update.callbackQuery?.let { callback ->
                    val callbackId = callback.id ?: return@let

                    service.answerCallbackQuery(callbackId)

                    val data = callback.data ?: return@let
                    val chatId = callback.message?.chat?.id ?: return@let
                    val trainer = trainerManager.getTrainer(chatId)

                    if (!isAllowedCallbackData(data)) {
                        InputSecurity.logSuspiciousInput("callback.data.invalid", data, chatId)
                        service.sendMessage(chatId, "Некорректные данные callback")
                        return@let
                    }

                    if (InputSecurity.containsSuspiciousSqlPattern(data)) {
                        InputSecurity.logSuspiciousInput("callback.data", data, chatId)
                    }

                    when {
                        data == CALLBACK_STATS -> {
                            sendOrUpdateStatistics(
                                chatId = chatId,
                                trainerManager = trainerManager,
                                telegramBotService = service,
                                dynamicMessage = dynamicMessage,
                                animationFrameByChat = animationFrameByChat,
                                forceNewMessage = true
                            )
                        }

                        data == CALLBACK_LEARN -> {
                            checkNextQuestionAndSend(trainerManager, service, chatId, imageIndex, fileIdCache)
                        }

                        data == CALLBACK_RESET -> {
                            trainerManager.reset(chatId)
                            dynamicMessage.clear(chatId)
                            animationFrameByChat.remove(chatId)
                            service.sendMessage(chatId, "Прогресс сброшен ✅")
                            service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS, CALLBACK_RESET)
                        }

                        data.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                            val answerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()
                            if (answerIndex == null) {
                                service.sendMessage(chatId, "Некорректный ответ: $data")
                                return@let
                            }

                            val isCorrect = trainer.checkAnswer(answerIndex)
                            if (isCorrect) {
                                service.sendMessage(chatId, "Правильно!")
                                sendOrUpdateStatistics(
                                    chatId = chatId,
                                    trainerManager = trainerManager,
                                    telegramBotService = service,
                                    dynamicMessage = dynamicMessage,
                                    animationFrameByChat = animationFrameByChat,
                                    forceNewMessage = false
                                )
                            } else {
                                val correctWord = trainer.getCurrentCorrectWord()
                                service.sendMessage(
                                    chatId,
                                    if (correctWord != null)
                                        "Неправильно! ${correctWord.original} – это ${correctWord.translate}"
                                    else
                                        "Неправильно!"
                                )
                            }

                            checkNextQuestionAndSend(trainerManager, service, chatId, imageIndex, fileIdCache)
                        }

                        else -> service.sendMessage(chatId, "Неизвестная команда: $data")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun sendOrUpdateStatistics(
    chatId: Long,
    trainerManager: UserTrainerManager,
    telegramBotService: TelegramBotService,
    dynamicMessage: DynamicMessage,
    animationFrameByChat: MutableMap<Long, Int>,
    forceNewMessage: Boolean
) {
    val stats = trainerManager.getTrainer(chatId).getStatistics()
    val frame = nextFrame(chatId, animationFrameByChat)
    val statsText = buildStatisticsText(stats, frame)

    if (!forceNewMessage && dynamicMessage.hasMessage(chatId)) {
        val messageId = dynamicMessage.getMessageId(chatId)
        if (messageId != null) {
            val editResponse = telegramBotService.editMessage(chatId, messageId, statsText)
            val ok = telegramBotService.isOkResponse(editResponse)
            val notModified = telegramBotService.responseDescription(editResponse)
                ?.contains("message is not modified", ignoreCase = true) == true

            if (ok || notModified) {
                dynamicMessage.rememberText(chatId, statsText)
                return
            }
        }
    }

    val sendResponse = telegramBotService.sendMessage(chatId, statsText)
    telegramBotService.extractMessageId(sendResponse)?.let { messageId ->
        dynamicMessage.setMessageId(chatId, messageId)
    }
    dynamicMessage.rememberText(chatId, statsText)
}

private fun handleUndoCommand(
    chatId: Long,
    trainerManager: UserTrainerManager,
    telegramBotService: TelegramBotService,
    dynamicMessage: DynamicMessage,
    animationFrameByChat: MutableMap<Long, Int>
) {
    val trainer = trainerManager.getTrainer(chatId)
    val undone = trainer.undoLastCorrectAnswer()

    if (!undone) {
        telegramBotService.sendMessage(chatId, "Нечего откатывать: нет предыдущего правильного ответа")
        return
    }

    telegramBotService.sendMessage(chatId, "Откатил последний правильный ответ ↩")

    val messageId = dynamicMessage.getMessageId(chatId)
    if (messageId == null) return

    val rollbackText = dynamicMessage.rollbackText(chatId)
        ?: buildStatisticsText(trainer.getStatistics(), nextFrame(chatId, animationFrameByChat))

    val response = telegramBotService.editMessage(chatId, messageId, rollbackText)
    val ok = telegramBotService.isOkResponse(response)
    val notModified = telegramBotService.responseDescription(response)
        ?.contains("message is not modified", ignoreCase = true) == true

    if (ok || notModified) return

    // Если редактирование не удалось (например, старое сообщение), отправляем новое и продолжаем работать с ним.
    val freshText = buildStatisticsText(trainer.getStatistics(), nextFrame(chatId, animationFrameByChat))
    val sendResponse = telegramBotService.sendMessage(chatId, freshText)
    telegramBotService.extractMessageId(sendResponse)?.let { newMessageId ->
        dynamicMessage.setMessageId(chatId, newMessageId)
    }
    dynamicMessage.rememberText(chatId, freshText)
}

private fun nextFrame(chatId: Long, animationFrameByChat: MutableMap<Long, Int>): Int {
    val next = ((animationFrameByChat[chatId] ?: -1) + 1) % 4
    animationFrameByChat[chatId] = next
    return next
}

private fun buildStatisticsText(stats: Statistics, frame: Int): String {
    val spinner = "|/-\\"[frame]
    val progressBar = buildProgressBar(stats.percent, frame)
    return """
        Статистика $spinner
        Выучено ${stats.learnedCount} из ${stats.totalCount} слов
        Прогресс: $progressBar ${stats.percent}%
    """.trimIndent()
}

private fun buildProgressBar(percent: Int, frame: Int, width: Int = 20): String {
    val safePercent = percent.coerceIn(0, 100)
    val filled = (safePercent * width) / 100
    val emptySymbol = if (frame % 2 == 0) '-' else '.'

    return "[${"#".repeat(filled)}${emptySymbol.toString().repeat(width - filled)}]"
}

private fun handleDictionaryUpload(
    chatId: Long,
    documentFileId: String,
    documentUniqueId: String,
    originalFileName: String,
    service: TelegramBotService,
    trainerManager: UserTrainerManager
) {
    if (InputSecurity.containsSuspiciousSqlPattern(originalFileName)) {
        InputSecurity.logSuspiciousInput("document.file_name", originalFileName, chatId)
    }

    service.sendMessage(chatId, "Файл получен: $originalFileName. Скачиваю...")

    val filePath = service.getFilePath(documentFileId)
    if (filePath == null) {
        service.sendMessage(chatId, "Не смог получить file_path 😕")
        return
    }

    val downloadsDir = File("downloads").apply { if (!exists()) mkdirs() }

    val downloadedFile = File(downloadsDir, documentUniqueId)
    if (!downloadedFile.exists()) {
        service.downloadFile(filePath, downloadedFile.absolutePath)
    }

    val added = trainerManager.updateDictionary(downloadedFile)

    service.sendMessage(chatId, "Готово ✅ Добавлено слов: $added")
}


internal fun readDictionaryLinesWithFallback(dictionaryFile: File): List<String> {
    val bytes = dictionaryFile.readBytes()

    data class Candidate(
        val charset: Charset,
        val text: String,
        val replacementChars: Int,
        val separators: Int
    )

    val candidates = listOf(Charsets.UTF_8, Charset.forName("windows-1251"))
        .map { charset ->
            val text = bytes.toString(charset)
            Candidate(
                charset = charset,
                text = text,
                replacementChars = text.count { it == '\uFFFD' },
                separators = text.count { it == '|' }
            )
        }

    val best = candidates.minWithOrNull(
        compareBy<Candidate> { it.replacementChars }
            .thenByDescending { it.separators }
    ) ?: return emptyList()

    if (best.charset != Charsets.UTF_8) {
        println("INFO: dictionary decoding fallback charset=${best.charset.name()} file=${dictionaryFile.name}")
    }

    return best.text.lineSequence().toList()
}

fun checkNextQuestionAndSend(
    trainerManager: UserTrainerManager,
    telegramBotService: TelegramBotService,
    chatId: Long,
    imageIndex: ImageIndex,
    fileIdCache: ImageFileIdCache
) {
    val trainer = trainerManager.getTrainer(chatId)
    val question = trainer.getNextQuestion()

    if (question == null) {
        telegramBotService.sendMessage(chatId, "Все слова в словаре выучены")
        return
    }

    // 1) Картинка-подсказка (если есть)
    try {
        // В текущем формате словаря:
        // - question.questionWord.original = АНГЛИЙСКОЕ слово (мы его показываем в тексте вопроса)
        // - question.questionWord.translate = РУССКИЙ перевод (он в кнопках)
        // image_index.json сделан по английским ключам (cat/dog/...), значит берём original.
        val hintKey = question.questionWord.original.trim().lowercase()
        val hint = imageIndex.find(hintKey)

        val cached = fileIdCache.get(hintKey)

        if (hint != null) {
            if (!cached.isNullOrBlank()) {
                val resp = telegramBotService.sendPhotoByFileId(chatId, cached, hint.hasSpoiler)
                val ok = resp.contains("\"ok\":true")

                if (!ok) {
                    val file = ResourceFileExtractor.extractTo(hint.path)
                    val uploadResp = telegramBotService.sendPhotoByFile(chatId, file, hint.hasSpoiler)
                    telegramBotService.extractBestPhotoFileId(uploadResp)?.let { newId ->
                        fileIdCache.put(hintKey, newId)
                    }
                }
            } else {
                val file = ResourceFileExtractor.extractTo(hint.path)
                val uploadResp = telegramBotService.sendPhotoByFile(chatId, file, hint.hasSpoiler)
                telegramBotService.extractBestPhotoFileId(uploadResp)?.let { newId ->
                    fileIdCache.put(hintKey, newId)
                }
            }
        }
    } catch (e: Exception) {
        println("WARN: sendHint failed: ${e.message}")
        e.printStackTrace()
    }

    // 2) Сам вопрос
    telegramBotService.sendQuestion(chatId, question)
}

private fun isAllowedCallbackData(data: String): Boolean {
    return data in allowedCallbackValues || callbackAnswerRegex.matches(data)
}
