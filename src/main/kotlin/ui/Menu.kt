package ui

import model.Statistics

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

fun printStatistics(stats: Statistics) {
    println("Выучено ${stats.learnedCount} из ${stats.totalCount} слов | ${stats.percent}%\n")
}