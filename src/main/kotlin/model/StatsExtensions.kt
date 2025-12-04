package model

fun List<Word>.toTrainingStats(minCorrect: Int): TrainingStats {
    val learned = count { it.correctAnswersCount >= minCorrect }
    return TrainingStats(size, learned, minCorrect)
}