package trainer

import data.DictionaryRepository
import domain.WordTrainer
import model.Question
import model.Statistics

private const val WORDS_FILE_PATH = "src/main/resources/words.txt"
private const val MIN_CORRECT_ANSWERS = 3

class LearnWordsTrainer(
    private val repository: DictionaryRepository = DictionaryRepository(WORDS_FILE_PATH),
    private val minCorrect: Int = MIN_CORRECT_ANSWERS
) {
    private val dictionary: List<Word> = repository.load()

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

    fun checkAnswer(userAnswerIndex: Int): Boolean {
        val question = currentQuestion ?: return false
        return wordTrainer.checkAnswer(question, userAnswerIndex)
    }

    fun getCorrectWordForCurrentQuestion(): Word? {
        return currentQuestion?.questionWord
    }
}