package domain

import data.ImageHintsRepository
import model.ImageHint
import telegram.TelegramBotService
import java.io.File

class HintImageService(
    private val repo: ImageHintsRepository,
    private val bot: TelegramBotService
) {
    fun sendHintIfExists(word: String, chatId: Long) {
        val index = repo.load()
        val meta: ImageHint = index[word] ?: return

        meta.fileId?.let { existingId ->
            bot.sendPhotoByFileId(existingId, chatId, meta.hasSpoiler)
            return
        }

        val file = File("src/main/resources/${meta.path}")
        if (!file.exists()) return

        val responseJson = bot.sendPhoto(file, chatId, meta.hasSpoiler)

        val newFileId = extractLargestPhotoFileId(responseJson) ?: return

        index[word] = meta.copy(fileId = newFileId)
        repo.save(index)
    }

    private fun extractLargestPhotoFileId(json: String): String? {
        val regex = Regex("\"file_id\"\\s*:\\s*\"([^\"]+)\"")
        val matches = regex.findAll(json).toList()
        return matches.lastOrNull()?.groupValues?.get(1)
    }
}