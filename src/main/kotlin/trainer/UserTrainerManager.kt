package trainer

import data.DatabaseUserDictionary
import data.DictionaryDataSource
import java.io.File

class UserTrainerManager(
    private val baseWordsFilePath: String,
    dbFilePath: String,
    private val minCorrect: Int = 3
) {
    private val dataSource = DictionaryDataSource(dbFilePath)
    private val trainers = mutableMapOf<Long, LearnWordsTrainer>()

    init {
        seedBaseDictionary()
    }

    fun getTrainer(chatId: Long): LearnWordsTrainer {
        return trainers.getOrPut(chatId) {
            val userDictionary = DatabaseUserDictionary(
                dataSource = dataSource,
                chatId = chatId,
                learningThreshold = minCorrect
            )
            LearnWordsTrainer(userDictionary = userDictionary, minCorrect = minCorrect)
        }
    }

    fun reset(chatId: Long) {
        trainers.remove(chatId)

        val userDictionary = DatabaseUserDictionary(
            dataSource = dataSource,
            chatId = chatId,
            learningThreshold = minCorrect
        )
        userDictionary.resetUserProgress()
    }

    fun updateDictionary(wordsFile: File): Int {
        val added = dataSource.updateDictionary(wordsFile)
        trainers.clear()
        return added
    }

    private fun seedBaseDictionary() {
        val resourceName = File(baseWordsFilePath).name
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourceName)

        if (stream != null) {
            val tempFile = kotlin.io.path.createTempFile(prefix = "base_words_", suffix = ".txt").toFile()
            tempFile.writeText(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            dataSource.updateDictionary(tempFile)
            tempFile.delete()
            return
        }

        val baseFile = File(baseWordsFilePath)
        require(baseFile.exists()) {
            "Base words file not found: ${baseFile.absolutePath} (and resource '$resourceName' not found)"
        }
        dataSource.updateDictionary(baseFile)
    }
}
