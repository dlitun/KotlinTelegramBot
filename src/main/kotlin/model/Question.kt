package model

import trainer.Word

data class Question(
    val questionWord: Word,
    val options: List<Word>
) {
    val correctOptionIndex: Int
        get() = options.indexOf(questionWord)
}