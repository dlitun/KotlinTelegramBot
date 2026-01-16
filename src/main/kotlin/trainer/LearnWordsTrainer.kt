package trainer

import model.Statistics

class LearnWordsTrainer(
    private val dictionary: List<Word> = emptyList(),
    private val learnedAnswerCount: Int = 3
) {

    fun start(): String {
        return "Начинаем изучение слов"
    }

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
}