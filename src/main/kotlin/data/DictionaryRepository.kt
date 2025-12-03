package data

import model.Word
import java.io.File

class DictionaryRepository(
    private val filePath: String
) {
    fun load(): List<Word> {
        return File(filePath)
            .readLines()
            .map { line ->
                val parts = line.split("|")
                Word(
                    original = parts[0],
                    translate = parts[1],
                    correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
                )
            }
    }

    fun save(dictionary: List<Word>) {
        val lines = dictionary.map { word ->
            "${word.original}|${word.translate}|${word.correctAnswersCount}"
        }
        File(filePath).writeText(lines.joinToString("\n"))
    }
}


