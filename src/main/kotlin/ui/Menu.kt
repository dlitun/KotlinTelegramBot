package ui

import model.TrainingStats

fun printMenu() {
    println(
        """
        Меню:
        1 – Учить слова
        2 – Статистика
        0 – Выход
        """.trimIndent()
    )
}

fun printStatistics(stats: TrainingStats) {
    println("Выучено ${stats.learnedWords} из ${stats.totalWords} слов | ${stats.learnedPercent}%\n")
}