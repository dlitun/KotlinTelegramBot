package ui

import domain.WordTrainer

fun runStudyMode(trainer: WordTrainer) {
    while (true) {
        if (!trainer.hasUnlearnedWords()) {
            println("Все слова выучены!\n")
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

        val isCorrect = trainer.checkAnswer(userAnswer - 1)

        if (isCorrect) {
            println("Правильно!\n")
        } else {
            println(
                "Неправильно! ${question.questionWord.original} – это ${question.questionWord.translate}\n"
            )
        }
    }
}