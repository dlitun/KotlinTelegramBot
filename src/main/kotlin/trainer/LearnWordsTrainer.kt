package trainer

import model.Statistics

class LearnWordsTrainer {

    private var learnedWords = 0
    private var totalWords = 0

    fun start(): String {
        return "Начинаем изучение слов"
    }

    fun getStatistics(): Statistics {
        val percent =
            if (totalWords == 0) 0 else (learnedWords * 100) / totalWords

        return Statistics(
            totalCount = totalWords,
            learnedCount = learnedWords,
            percent = percent
        )
    }

    fun setProgress(learned: Int, total: Int) {
        learnedWords = learned
        totalWords = total
    }
}