package org.example

import java.io.File

private const val MIN_CORRECT = 3
private const val MENU_STUDY = 1
private const val MENU_STATS = 2
private const val MENU_EXIT = 0

private const val DICTIONARY_PATH = "src/main/resources/words.txt"

data class Word(
    val original: String,
    val translate: String,
    val correctAnswersCount: Int = 0
)

class DictionaryRepository(
    private val filePath: String
) {
    fun load(): MutableList<Word> {
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
            .toMutableList()
    }

    fun save(dictionary: List<Word>) {
        val lines = dictionary.map { word ->
            "${word.original}|${word.translate}|${word.correctAnswersCount}"
        }
        File(filePath).writeText(lines.joinToString("\n"))
    }
}

data class TrainingStats(
    val totalWords: Int,
    val learnedWords: Int,
    val minCorrectAnswers: Int
) {
    val learnedPercent: Int
        get() = if (totalWords > 0) learnedWords * 100 / totalWords else 0
}

fun List<Word>.toTrainingStats(minCorrectAnswers: Int): TrainingStats {
    val learned = count { it.correctAnswersCount >= minCorrectAnswers }
    return TrainingStats(size, learned, minCorrectAnswers)
}

data class Question(
    val questionWord: Word,
    val options: List<Word>
) {
    val correctOptionIndex: Int
        get() = options.indexOf(questionWord)
}

class WordTrainer(
    private val dictionary: MutableList<Word>,
    private val repository: DictionaryRepository,
    private val minCorrect: Int = MIN_CORRECT
) {
    fun hasUnlearnedWords(): Boolean =
        dictionary.any { it.correctAnswersCount < minCorrect }

    fun createQuestion(): Question {
        val notLearned = dictionary.filter { it.correctAnswersCount < minCorrect }
        if (notLearned.isEmpty()) throw IllegalStateException("Нет слов для тренировки")
        val options = notLearned.shuffled().take(4)
        val correct = options.random()
        return Question(correct, options)
    }

    fun checkAnswer(question: Question, answerIndex: Int): Boolean {
        val isCorrect = answerIndex == question.correctOptionIndex
        if (isCorrect) incrementCorrectAnswer(question.questionWord)
        return isCorrect
    }

    private fun incrementCorrectAnswer(word: Word) {
        val index = dictionary.indexOfFirst {
            it.original == word.original && it.translate == word.translate
        }
        if (index != -1) {
            val old = dictionary[index]
            dictionary[index] = old.copy(
                correctAnswersCount = old.correctAnswersCount + 1
            )
            repository.save(dictionary)
        }
    }
}

fun String.toIntOrNullInRange(range: IntRange): Int? {
    val n = this.toIntOrNull() ?: return null
    return if (n in range) n else null
}

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

fun readMenuChoice(): Int? {
    print("Ваш выбор: ")
    val input = readln()
    return input.toIntOrNullInRange(0..2)
}

fun printStatistics(stats: TrainingStats) {
    println("Выучено ${stats.learnedWords} из ${stats.totalWords} слов | ${stats.learnedPercent}%\n")
}

fun readAnswerNumber(maxOption: Int): Int? {
    print("Ваш ответ (введите номер варианта): ")
    val input = readln()
    val n = input.toIntOrNullInRange(0..maxOption)
    if (n == null) println("Введите число от 0 до $maxOption\n")
    return n
}

fun runStudyMode(trainer: WordTrainer) {
    while (true) {
        if (!trainer.hasUnlearnedWords()) {
            println("Все слова в словаре выучены\n")
            return
        }

        val question = trainer.createQuestion()

        println()
        println("${question.questionWord.original}:")
        question.options.forEachIndexed { index, word ->
            println(" ${index + 1} - ${word.translate}")
        }
        println(" ----------")
        println(" 0 - Меню\n")

        val userAnswer = readAnswerNumber(question.options.size) ?: continue

        if (userAnswer == 0) {
            println()
            return
        }

        val isCorrect = trainer.checkAnswer(question, userAnswer - 1)

        if (isCorrect) {
            println("Правильно!\n")
        } else {
            println(
                "Неправильно! ${question.questionWord.original} – это ${question.questionWord.translate}\n"
            )
        }
    }
}

fun main() {
    val repository = DictionaryRepository(DICTIONARY_PATH)
    val dictionary = repository.load()
    val trainer = WordTrainer(dictionary, repository)

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