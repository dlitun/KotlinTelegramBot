package trainer

import data.IUserDictionary
import domain.WordTrainer
import model.Question
import model.Statistics

class LearnWordsTrainer(
    private val userDictionary: IUserDictionary,
    private val minCorrect: Int = 3
) {
    private val wordTrainer = WordTrainer(
        userDictionary = userDictionary,
        minCorrect = minCorrect
    )

    fun getStatistics(): Statistics {
        return wordTrainer.getStatistics()
    }

    fun getNextQuestion(): Question? {
        return wordTrainer.getNextQuestion()
    }

    fun checkAnswer(answerIndex: Int): Boolean {
        return wordTrainer.checkAnswer(answerIndex)
    }

    fun undoLastCorrectAnswer(): Boolean {
        return wordTrainer.undoLastCorrectAnswer()
    }

    fun getCurrentCorrectWord(): Word? {
        return wordTrainer.getCurrentCorrectWord()
    }

    fun resetProgress() {
        userDictionary.resetUserProgress()
    }
}