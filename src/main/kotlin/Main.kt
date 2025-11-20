package org.example

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    val correctAnswersCount: Int = 0
)

fun loadDictionary(): List<Word> {
    val lines = File("src/main/resources/words.txt").readLines()
    val dictionary = mutableListOf<Word>()

    for (line in lines) {
        val parts = line.split("|")
        val original = parts[0]
        val translation = parts[1]
        val correctAnswers = parts.getOrNull(2)?.toInt() ?: 0
        val word = Word(original, translation, correctAnswers)
        dictionary.add(word)
    }

    return dictionary
}

fun main() {
    val dictionary = loadDictionary()

    while (true) {
        println("\nМеню:")
        println("1 – Учить слова")
        println("2 – Статистика")
        println("0 – Выход")
        print("Ваш выбор: ")

        val input = readln()

        when (input) {
            "1" -> {
                println("Вы выбрали: Учить слова")
            }

            "2" -> {
                val learnedWords = dictionary.filter { it.correctAnswersCount >= 3 }
                val totalCount = dictionary.size
                val learnedCount = learnedWords.size
                val percent = if (totalCount > 0) {
                    learnedCount * 100 / totalCount
                } else {
                    0
                }

                println("Выучено $learnedCount из $totalCount слов | $percent%")
                println()
            }

            "0" -> {
                println("Выход из программы...")
                break
            }

            else -> println("Введите число 1, 2 или 0")
        }
    }
}