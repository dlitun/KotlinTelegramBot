package ui

import data.FileUserDictionary
import domain.WordTrainer

private const val MIN_CORRECT = 3

fun main() {
    val userDictionary = FileUserDictionary("words.txt", MIN_CORRECT)

    val trainer = WordTrainer(
        userDictionary = userDictionary,
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
                val stats = trainer.getStatistics()
                printStatistics(stats)
            }

            0 -> {
                println("Выход из программы…")
                return
            }
        }
    }
}