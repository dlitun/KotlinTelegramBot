package org.example

import java.io.File

private const val MIN_CORRECT = 3
private const val MENU_STUDY = 1
private const val MENU_STATS = 2
private const val MENU_EXIT = 0

data class Word(
    val original: String,
    val translate: String,
    val correctAnswersCount: Int = 0
)

fun loadDictionary(): List<Word> {
    return File("src/main/resources/words.txt")
        .readLines()
        .map { line ->
            val parts = line.split("|")
            Word(
                original = parts[0],
                translate = parts[1],
                correctAnswersCount = parts.getOrNull(2)?.toInt() ?: 0
            )
        }
}

fun main() {
    val dictionary = loadDictionary()

    while (true) {
        println(
            """
            Меню:
            1 – Учить слова
            2 – Статистика
            0 – Выход
            """.trimIndent()
        )
        print("Ваш выбор: ")

        val input = readln().toIntOrNull()

        when (input) {

            MENU_STUDY -> {
                println("Вы выбрали: Учить слова")
            }

            MENU_STATS -> {
                val learnedWords = dictionary.filter { it.correctAnswersCount >= MIN_CORRECT }
                val total = dictionary.size
                val learned = learnedWords.size
                val percent = if (total > 0) learned * 100 / total else 0

                println("Выучено $learned из $total слов | $percent%\n")
            }

            MENU_EXIT -> {
                println("Выход из программы…")
                break
            }

            else -> println("Введите 1, 2 или 0")
        }
    }
}