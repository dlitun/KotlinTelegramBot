package telegram

import data.DictionaryRepository
import trainer.UserTrainerManager
import trainer.Word
import java.io.File

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
        try {
            Thread.sleep(2000)

            val updates = service.getUpdates(offset)
            if (updates.isEmpty()) continue
            offset = updates.maxOf { it.updateId } + 1

            println("DEBUG: got updates count=${updates.size} newOffset=$offset")

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

                    val rawText = message.text ?: return@let
                    val normalizedText = rawText.trim()
                    val command = normalizedText
                        .substringBefore(' ')          // убираем аргументы: "/start foo"
                        .substringBefore('@')          // убираем суффикс: "/start@MyBot"

                    println("DEBUG: message chatId=$chatId textRaw='$rawText' command='$command'")

                    when (command) {
                        START_COMMAND -> {
                            println("DEBUG: START_COMMAND matched -> calling sendMenu")
                            service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS, CALLBACK_RESET)
                        }

                        RESET_COMMAND -> {
                            trainerManager.reset(chatId)
                            service.sendMessage(chatId, "Прогресс сброшен ✅")
                            service.sendMenu(chatId, CALLBACK_LEARN, CALLBACK_STATS, CALLBACK_RESET)
                        }
                    }
                }

                update.callbackQuery?.let { callback ->
                    val callbackId = callback.id ?: return@let

                    service.answerCallbackQuery(callbackId)

                    val data = callback.data ?: return@let
                    val chatId = callback.message?.chat?.id ?: return@let

                    println("DEBUG: callbackQuery chatId=$chatId data='$data'")

                    val trainer = trainerManager.getTrainer(chatId)

                    when {
                        data == CALLBACK_STATS -> {
                            val stats = trainer.getStatistics()
                            service.sendMessage(
                                chatId,
                                "Выучено ${stats.learnedCount} из ${stats.totalCount} слов | ${stats.percent}%"
                            )
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
                                return@let
                            }

                            val isCorrect = trainer.checkAnswer(answerIndex)
                            if (isCorrect) {
                                service.sendMessage(chatId, "Правильно!")
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

                            checkNextQuestionAndSend(trainerManager, service, chatId)
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

private fun handleDictionaryUpload(
    chatId: Long,
    documentFileId: String,
    documentUniqueId: String,
    originalFileName: String,
    service: TelegramBotService,
    trainerManager: UserTrainerManager
) {
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

    val added = mergeWordsIntoUserDictionary(chatId, downloadedFile, trainerManager)

    service.sendMessage(chatId, "Готово ✅ Добавлено слов: $added")
}

private fun mergeWordsIntoUserDictionary(
    chatId: Long,
    dictionaryFile: File,
    trainerManager: UserTrainerManager
): Int {
    val userFile = File("users", "words_$chatId.txt")
    if (!userFile.exists()) {
        trainerManager.getTrainer(chatId)
    }

    val repo = DictionaryRepository(userFile.absolutePath)
    val current = repo.load()

    val existingKeys = current
        .map { "${it.original}|${it.translate}" }
        .toHashSet()

    var added = 0

    dictionaryFile.readLines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { line ->
            val parts = line.split("|")
            if (parts.size < 2) return@forEach

            val original = parts[0].trim()
            val translate = parts[1].trim()
            if (original.isBlank() || translate.isBlank()) return@forEach

            val key = "$original|$translate"
            if (existingKeys.contains(key)) return@forEach

            current.add(Word(original = original, translate = translate, correctAnswersCount = 0))
            existingKeys.add(key)
            added++
        }

    repo.save(current)

    return added
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