package trainer

import data.DictionaryRepository
import domain.WordTrainer
import model.Question
import model.Statistics

class LearnWordsTrainer(
    private val filePath: String = "src/main/resources/words.txt",
    private val learnedAnswerCount: Int = 3
) {
    private val repository = DictionaryRepository(filePath)

    private val dictionary: MutableList<Word> = repository.load().toMutableList()

    private val wordTrainer = WordTrainer(
        dictionary = dictionary,
        repository = repository,
        minCorrect = learnedAnswerCount
    )

    fun start(): String = "Начинаем изучение слов"

    fun getStatistics(): Statistics {
        val totalCount = dictionary.size
        val learnedCount = dictionary.count { it.correctAnswersCount >= learnedAnswerCount }
        val percent = if (totalCount == 0) 0 else (learnedCount * 100) / totalCount

        return Statistics(
            totalCount = totalCount,
            learnedCount = learnedCount,
            percent = percent
        )
    }

    fun getNextQuestion(): Question? {
        if (!wordTrainer.hasUnlearnedWords()) return null
        return wordTrainer.createQuestion()
    }

    fun checkAnswer(question: Question, answerIndex: Int): Boolean {
        return wordTrainer.checkAnswer(question, answerIndex)
    }
}
