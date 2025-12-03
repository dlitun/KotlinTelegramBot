package model

data class TrainingStats(
    val totalWords: Int,
    val learnedWords: Int,
    val minCorrectAnswers: Int
) {
    val learnedPercent: Int
        get() = if (totalWords > 0) learnedWords * 100 / totalWords else 0
}