package domain

import data.DictionaryRepository
import model.Question
import model.Statistics
import trainer.Word

class WordTrainer(
    private val dictionary: MutableList<Word>,
    private val repository: DictionaryRepository,
    private val minCorrect: Int
) {
    private var currentQuestion: Question? = null

    fun hasUnlearnedWords(): Boolean =
        dictionary.any { it.correctAnswersCount < minCorrect }

    fun createQuestion(): Question {
        val notLearned = dictionary.filter { it.correctAnswersCount < minCorrect }
        if (notLearned.isEmpty()) throw IllegalStateException("Нет слов для тренировки")

        val options = notLearned.shuffled().take(4)
        val correct = options.random()

        return Question(correct, options)
    }

    fun getNextQuestion(): Question? {
        if (!hasUnlearnedWords()) {
            currentQuestion = null
            return null
        }
        currentQuestion = createQuestion()
        return currentQuestion
    }

    fun checkAnswer(answerIndex: Int): Boolean {
        val q = currentQuestion ?: return false
        val isCorrect = answerIndex == q.correctOptionIndex
        if (isCorrect) incrementCorrectAnswer(q.questionWord)
        return isCorrect
    }

    fun getCurrentCorrectWord(): Word? = currentQuestion?.questionWord

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

    private fun incrementCorrectAnswer(word: Word) {
        val index = dictionary.indexOfFirst {
            it.original == word.original && it.translate == word.translate
        }

        if (index != -1) {
            dictionary[index].correctAnswersCount++
            repository.save(dictionary)
        }
    }
}