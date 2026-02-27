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

        println("DEBUG: sendHintIfExists word='$word' key='$key'")
        println("DEBUG: index keys=${index.keys}")

        val meta: ImageHint = index[key] ?: run {
            println("DEBUG: meta NOT FOUND for key='$key'")
            return
        }

        println("DEBUG: meta FOUND path='${meta.path}' fileId='${meta.fileId}' hasSpoiler=${meta.hasSpoiler}")

        meta.fileId?.let { existingId ->
            println("DEBUG: sending by fileId=$existingId")
            val response = bot.sendPhotoByFileId(existingId, chatId, meta.hasSpoiler)
            println("DEBUG: sendPhotoByFileId response=$response")
            return
        }

        val resourcePath = meta.path.removePrefix("/")
        println("DEBUG: loading resource='$resourcePath'")

        val stream = this::class.java.classLoader.getResourceAsStream(resourcePath)

        if (stream == null) {
            println("DEBUG: resource NOT FOUND: '$resourcePath'")
            return
        }

        val bytes = stream.use { it.readBytes() }

        println("DEBUG: resource loaded bytes=${bytes.size}")

        if (bytes.isEmpty()) {
            println("DEBUG: bytes EMPTY for '$resourcePath'")
            return
        }

        val fileName = resourcePath.substringAfterLast('/')
        println("DEBUG: calling bot.sendPhoto fileName='$fileName'")

        val responseJson = bot.sendPhoto(
            fileName = fileName,
            bytes = bytes,
            chatId = chatId,
            hasSpoiler = meta.hasSpoiler
        )

        println("DEBUG: sendPhoto response=$responseJson")

        val newFileId = extractLargestPhotoFileId(responseJson) ?: run {
            println("DEBUG: fileId NOT extracted from response")
            return
        }

        println("DEBUG: extracted fileId=$newFileId")

        index[key] = meta.copy(fileId = newFileId)
        repo.save(index)

        println("DEBUG: saved fileId to cache for key='$key'")
    }

    private fun extractLargestPhotoFileId(json: String): String? {
        val regex = Regex("\"file_id\"\\s*:\\s*\"([^\"]+)\"")
        val matches = regex.findAll(json).toList()
        return matches.lastOrNull()?.groupValues?.get(1)
    }
}