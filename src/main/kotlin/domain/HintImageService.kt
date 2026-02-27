package domain

import data.ImageHintsRepository
import model.ImageHint
import telegram.TelegramBotService

class HintImageService(
    private val repo: ImageHintsRepository,
    private val bot: TelegramBotService
) {
    fun sendHintIfExists(word: String, chatId: Long) {
        val index = repo.load()
        val key = word.trim().lowercase()

        val meta: ImageHint = index[key] ?: return

        meta.fileId?.let { existingId ->
            bot.sendPhotoByFileId(existingId, chatId, meta.hasSpoiler)
            return
        }

        val resourcePath = meta.path.removePrefix("/")
        val bytes = this::class.java.classLoader.getResourceAsStream(resourcePath)
            ?.use { it.readBytes() }
            ?: return

        if (bytes.isEmpty()) return

        val fileName = resourcePath.substringAfterLast('/')
        val responseJson = bot.sendPhoto(fileName = fileName, bytes = bytes, chatId = chatId, hasSpoiler = meta.hasSpoiler)

        val newFileId = extractLargestPhotoFileId(responseJson) ?: return

        index[key] = meta.copy(fileId = newFileId)
        repo.save(index)
    }

    private fun extractLargestPhotoFileId(json: String): String? {
        val regex = Regex("\"file_id\"\\s*:\\s*\"([^\"]+)\"")
        val matches = regex.findAll(json).toList()
        return matches.lastOrNull()?.groupValues?.get(1)
    }
}