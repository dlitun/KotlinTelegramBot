package trainer

class LearnWordsTrainer {

    private var learnedWords: Int = 0
    private var totalWords: Int = 0

    fun start(): String {
        return "Начинаем изучение слов"
    }

    fun getStatistics(): String {
        val percent = if (totalWords == 0) 0 else (learnedWords * 100) / totalWords
        return "Выучено $learnedWords из $totalWords слов | $percent%"
    }

    fun setProgress(learned: Int, total: Int) {
        learnedWords = learned
        totalWords = total
    }
}