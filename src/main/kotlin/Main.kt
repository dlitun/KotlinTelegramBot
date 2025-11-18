package org.example

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    val correctAnswersCount: Int = 0
)

fun main() {
    val lines = File("src/main/resources/words.txt").readLines()
    val dictionary = mutableListOf<Word>()

    for (line in lines) {
        val parts = line.split("|")

        val original = parts[0]
        val translation = parts[1]

        val correctAnswers =
            if (parts.size == 3) parts[2].toInt()
            else 0

        val word = Word(original, translation, correctAnswers)
        dictionary.add(word)
    }

    for (word in dictionary) {
        println(word)
    }
}