package data

import trainer.Word

class DatabaseUserDictionary(
    private val dataSource: DictionaryDataSource,
    private val chatId: Long,
    private val learningThreshold: Int = 3,
    username: String? = null,
) : IUserDictionary {

    init {
        dataSource.ensureUser(chatId, username)
    }

    override fun getNumOfLearnedWords(): Int =
        dataSource.getNumOfLearnedWords(chatId, learningThreshold)

    override fun getSize(): Int = dataSource.getSize()

    override fun getLearnedWords(): List<Word> =
        dataSource.getLearnedWords(chatId, learningThreshold)

    override fun getUnlearnedWords(): List<Word> =
        dataSource.getUnlearnedWords(chatId, learningThreshold)

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        dataSource.setCorrectAnswersCount(chatId, word, correctAnswersCount)
    }

    override fun resetUserProgress() {
        dataSource.resetUserProgress(chatId)
    }

    fun getCorrectAnswersCount(word: String): Int =
        dataSource.getCorrectAnswersCount(chatId, word)
}

