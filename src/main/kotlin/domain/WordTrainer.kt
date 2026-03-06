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
    private val correctAnswerHistory = ArrayDeque<Pair<String, String>>()

    fun hasUnlearnedWords(): Boolean =
        dictionary.any { it.correctAnswersCount < minCorrect }

    fun getNextQuestion(): Question? {
        if (!hasUnlearnedWords()) {
            currentQuestion = null
            return null
        }

        currentQuestion = createQuestion()
        return currentQuestion
    }

    fun createQuestion(): Question {
        val notLearned = dictionary.filter { it.correctAnswersCount < minCorrect }
        val options = notLearned.shuffled().take(4)
        val correct = options.random()
        return Question(correct, options)
    }

    fun checkAnswer(answerIndex: Int): Boolean {
        val question = currentQuestion ?: return false
        val isCorrect = answerIndex == question.correctOptionIndex

        if (isCorrect) {
            incrementCorrectAnswer(question.questionWord)
        }

        return isCorrect
    }

    fun undoLastCorrectAnswer(): Boolean {
        val lastCorrect = correctAnswerHistory.removeLastOrNull() ?: return false

        val index = dictionary.indexOfFirst {
            it.original == lastCorrect.first && it.translate == lastCorrect.second
        }

        if (index == -1) return false
        if (dictionary[index].correctAnswersCount <= 0) return false

        dictionary[index].correctAnswersCount--
        repository.save(dictionary)
        return true
    }

    fun getCurrentCorrectWord(): Word? =
        currentQuestion?.questionWord

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
            correctAnswerHistory.addLast(dictionary[index].original to dictionary[index].translate)
            repository.save(dictionary)
        }
    }
}