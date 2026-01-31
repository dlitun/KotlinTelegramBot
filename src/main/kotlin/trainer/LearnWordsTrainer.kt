package trainer

import data.DictionaryRepository
import domain.WordTrainer
import model.Question
import model.Statistics

class LearnWordsTrainer(
    private val repository: DictionaryRepository,
    private val minCorrect: Int = 3
) {
    private val dictionary: MutableList<Word> = repository.load()

    private val wordTrainer: WordTrainer = WordTrainer(
        dictionary = dictionary,
        repository = repository,
        minCorrect = minCorrect
    )

    private var currentQuestion: Question? = null

    fun getStatistics(): Statistics {
        val totalCount = dictionary.size
        val learnedCount = dictionary.count { it.correctAnswersCount >= minCorrect }
        val percent = if (totalCount == 0) 0 else (learnedCount * 100) / totalCount

        return Statistics(
            totalCount = totalCount,
            learnedCount = learnedCount,
            percent = percent
        )
    }

    fun getNextQuestion(): Question? {
        if (!wordTrainer.hasUnlearnedWords()) {
            currentQuestion = null
            return null
        }

        val question = wordTrainer.createQuestion()
        currentQuestion = question
        return question
    }

    fun checkAnswer(answerIndex: Int): Boolean {
        val question = currentQuestion ?: return false
        return wordTrainer.checkAnswer(question, answerIndex)
    }

    fun getCurrentCorrectWord(): Word? {
        return currentQuestion?.questionWord
    }
}