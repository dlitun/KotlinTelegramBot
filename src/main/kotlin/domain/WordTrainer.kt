package domain

import data.DictionaryRepository
import model.Question
import model.Word

class WordTrainer(
    private val dictionary: List<Word>,
    private val repository: DictionaryRepository,
    private val minCorrect: Int
) {
    fun hasUnlearnedWords(): Boolean =
        dictionary.any { it.correctAnswersCount < minCorrect }

    fun createQuestion(): Question {
        val notLearned = dictionary.filter { it.correctAnswersCount < minCorrect }
        if (notLearned.isEmpty()) throw IllegalStateException("Нет слов для тренировки")

        val options = notLearned.shuffled().take(4)
        val correct = options.random()

        return Question(correct, options)
    }

    fun checkAnswer(question: Question, answerIndex: Int): Boolean {
        val isCorrect = answerIndex == question.correctOptionIndex
        if (isCorrect) incrementCorrectAnswer(question.questionWord)
        return isCorrect
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