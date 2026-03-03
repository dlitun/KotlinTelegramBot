package trainer

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
            val repository = data.DictionaryRepository(userFile.absolutePath)
            LearnWordsTrainer(repository = repository, minCorrect = minCorrect)
        }
    }

    fun reset(chatId: Long) {
        trainers.remove(chatId)

        val userFile = ensureUserDictionaryFile(chatId)
        val repository = data.DictionaryRepository(userFile.absolutePath)
        repository.resetProgress()
    }

    private fun ensureUserDictionaryFile(chatId: Long): File {
        val dir = File(usersDirPath)
        if (!dir.exists()) dir.mkdirs()

        val userFile = File(dir, "words_$chatId.txt")

        if (!userFile.exists()) {
            val text = loadBaseWordsText()
            userFile.writeText(text)

            val repo = data.DictionaryRepository(userFile.absolutePath)
            repo.resetProgress()
        }

        return userFile
    }

    private fun loadBaseWordsText(): String {
        // 1) Пытаемся взять из jar resources (то, что нужно на VPS)
        val resourceName = File(baseWordsFilePath).name
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourceName)
        if (stream != null) {
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            println("DEBUG: base words loaded from resources: $resourceName (chars=${text.length})")
            return text
        }

        // 2) Fallback: путь на диске (может понадобиться при кастомном запуске)
        val baseFile = File(baseWordsFilePath)
        require(baseFile.exists()) { "Base words file not found: ${baseFile.absolutePath} (and resource '$resourceName' not found)" }
        val text = baseFile.readText()
        println("DEBUG: base words loaded from file: ${baseFile.absolutePath} (chars=${text.length})")
        return text
    }
}
