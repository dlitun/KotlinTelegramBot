package ui

import data.DictionaryRepository
import domain.WordTrainer
import model.toTrainingStats

private const val MIN_CORRECT = 3

fun main() {
    val repository = DictionaryRepository("src/main/resources/words.txt")
    val dictionary = repository.load().toMutableList()

    val trainer = WordTrainer(
        dictionary = dictionary,
        repository = repository,
        minCorrect = MIN_CORRECT
    )

    while (true) {
        printMenu()

        val choice = readMenuChoice()
        if (choice == null) {
            continue
        }

        when (choice) {
            1 -> {
                runStudyMode(trainer)
            }

            2 -> {
                val stats = dictionary.toTrainingStats(MIN_CORRECT)
                printStatistics(stats)
            }

            0 -> {
                println("Выход из программы…")
                return
            }
        }
    }
}