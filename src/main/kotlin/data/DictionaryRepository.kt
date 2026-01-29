package data

import trainer.Word
import java.io.File

class DictionaryRepository(private val filePath: String) {

    fun load(): MutableList<Word> {
        val file = File(filePath)
        if (!file.exists()) return mutableListOf()

        return file.readLines()
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split("|")
                Word(
                    original = parts[0],
                    translate = parts[1],
                    correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
                )
            }
            .toMutableList()
    }

    fun save(dictionary: List<Word>) {
        val lines = dictionary.map { word ->
            "${word.original}|${word.translate}|${word.correctAnswersCount}"
        }
        File(filePath).writeText(lines.joinToString("\n"))
    }

    fun resetProgress() {
        val current = load()
        val cleared = current.map { it.copy(correctAnswersCount = 0) }
        save(cleared)
    }
}