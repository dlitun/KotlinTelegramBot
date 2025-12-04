package org.example

import data.DictionaryRepository
import domain.WordTrainer
import model.toTrainingStats
import ui.printMenu
import ui.printStatistics
import ui.readMenuChoice
import ui.runStudyMode

private const val MIN_CORRECT = 3
private const val MENU_STUDY = 1
private const val MENU_STATS = 2
private const val MENU_EXIT = 0

private const val DICTIONARY_PATH = "src/main/resources/words.txt"

fun main() {
    val repository = DictionaryRepository(DICTIONARY_PATH)
    val dictionary = repository.load()
    val trainer = WordTrainer(dictionary, repository, MIN_CORRECT)

    while (true) {
        printMenu()
        val choice = readMenuChoice()

        when (choice) {
            MENU_STUDY -> {
                println("Вы выбрали: Учить слова")
                runStudyMode(trainer)
            }

            MENU_STATS -> {
                val stats = dictionary.toTrainingStats(MIN_CORRECT)
                printStatistics(stats)
            }

            MENU_EXIT -> {
                println("Выход из программы…")
                return
            }

            else -> println("Введите 1, 2 или 0\n")
        }
    }
}