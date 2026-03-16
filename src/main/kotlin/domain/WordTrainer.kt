package domain

import data.IUserDictionary
import model.Question
import model.Statistics
import trainer.Word

class WordTrainer(
    private val userDictionary: IUserDictionary,
    private val minCorrect: Int
) {
    private var currentQuestion: Question? = null
    private val correctAnswerHistory = ArrayDeque<Pair<String, Int>>()

    fun hasUnlearnedWords(): Boolean =
        userDictionary.getUnlearnedWords().isNotEmpty()

    fun getNextQuestion(): Question? {
        if (!hasUnlearnedWords()) {
            currentQuestion = null
            return null
        }

        currentQuestion = createQuestion()
        return currentQuestion
    }

    fun createQuestion(): Question {
        val notLearned = userDictionary.getUnlearnedWords()
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

        val currentWord = findWordByOriginal(lastCorrect.first) ?: return false
        if (currentWord.correctAnswersCount <= 0) return false

        userDictionary.setCorrectAnswersCount(lastCorrect.first, lastCorrect.second)
        return true
    }

    fun getCurrentCorrectWord(): Word? =
        currentQuestion?.questionWord

    fun getStatistics(): Statistics {
        val totalCount = userDictionary.getSize()
        val learnedCount = userDictionary.getNumOfLearnedWords()
        val percent = if (totalCount == 0) 0 else (learnedCount * 100) / totalCount

        return Statistics(
            totalCount = totalCount,
            learnedCount = learnedCount,
            percent = percent
        )
    }

    private fun incrementCorrectAnswer(word: Word) {
        val currentWord = findWordByOriginal(word.original) ?: return
        val previousCount = currentWord.correctAnswersCount
        userDictionary.setCorrectAnswersCount(word.original, previousCount + 1)
        correctAnswerHistory.addLast(word.original to previousCount)
    }

    private fun findWordByOriginal(original: String): Word? {
        val allWords = userDictionary.getLearnedWords() + userDictionary.getUnlearnedWords()
        return allWords.find { it.original == original }
    }
}