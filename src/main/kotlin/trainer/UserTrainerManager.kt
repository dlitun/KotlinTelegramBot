package trainer

import data.DictionaryRepository
import java.io.File

class UserTrainerManager(
    private val baseWordsFilePath: String,
    private val usersDirPath: String,
    private val minCorrect: Int = 3
) {
    private val trainers = mutableMapOf<Long, LearnWordsTrainer>()

    fun getTrainer(chatId: Long): LearnWordsTrainer {
        return trainers.getOrPut(chatId) {
            val userFile = ensureUserDictionaryFile(chatId)
            val repo = DictionaryRepository(userFile.absolutePath)
            LearnWordsTrainer(repository = repo, minCorrect = minCorrect)
        }
    }

    fun reset(chatId: Long) {
        val userFile = ensureUserDictionaryFile(chatId)
        val repo = DictionaryRepository(userFile.absolutePath)
        repo.resetProgress()
        trainers.remove(chatId)
    }

    private fun ensureUserDictionaryFile(chatId: Long): File {
        val dir = File(usersDirPath)
        if (!dir.exists()) dir.mkdirs()

        val userFile = File(dir, "words_$chatId.txt")
        if (!userFile.exists()) {
            val base = File(baseWordsFilePath)
            userFile.writeText(base.readText())
        }
        return userFile
    }
}