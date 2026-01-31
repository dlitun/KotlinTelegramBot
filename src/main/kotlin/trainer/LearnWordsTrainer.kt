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

    fun getStatistics(): Statistics {
        return wordTrainer.getStatistics()
    }

    fun getNextQuestion(): Question? {
        return wordTrainer.getNextQuestion()
    }

    fun checkAnswer(answerIndex: Int): Boolean {
        return wordTrainer.checkAnswer(answerIndex)
    }

    fun getCurrentCorrectWord(): Word? {
        return wordTrainer.getCurrentCorrectWord()
    }
}