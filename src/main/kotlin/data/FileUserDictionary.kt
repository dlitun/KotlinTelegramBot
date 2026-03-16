package data

import trainer.Word
import java.io.File

class FileUserDictionary(
    private val fileName: String,
    private val learningThreshold: Int = 3,
) : IUserDictionary {

    private val dictionary: MutableList<Word> = loadDictionary().toMutableList()

    override fun getNumOfLearnedWords(): Int =
        dictionary.count { it.correctAnswersCount >= learningThreshold }

    override fun getSize(): Int = dictionary.size

    override fun getLearnedWords(): List<Word> =
        dictionary.filter { it.correctAnswersCount >= learningThreshold }

    override fun getUnlearnedWords(): List<Word> =
        dictionary.filter { it.correctAnswersCount < learningThreshold }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        dictionary.find { it.original == word }?.let {
            it.correctAnswersCount = correctAnswersCount.coerceAtLeast(0)
            saveDictionary()
        }
    }

    override fun resetUserProgress() {
        dictionary.forEach { it.correctAnswersCount = 0 }
        saveDictionary()
    }

    fun getAllWords(): List<Word> = dictionary.toList()

    fun saveAll(words: List<Word>) {
        dictionary.clear()
        dictionary.addAll(words)
        saveDictionary()
    }

    private fun loadDictionary(): List<Word> {
        val wordsFile = File(fileName)
        if (!wordsFile.exists()) return emptyList()

        return wordsFile.readLines()
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split("|")
                require(parts.size >= 2) { "Некорректная строка в словаре: '$line'" }

                Word(
                    original = parts[0],
                    translate = parts[1],
                    correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
                )
            }
    }

    private fun saveDictionary() {
        val file = File(fileName)
        val newFileContent = dictionary.map { "${it.original}|${it.translate}|${it.correctAnswersCount}" }
        file.writeText(newFileContent.joinToString(separator = "\n"))
    }
}

