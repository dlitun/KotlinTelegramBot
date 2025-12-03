package ui

fun String.toIntOrNullInRange(range: IntRange): Int? {
    val n = this.toIntOrNull() ?: return null
    return if (n in range) n else null
}

fun readMenuChoice(): Int? {
    print("Ваш выбор: ")
    val input = readln()
    return input.toIntOrNullInRange(0..2)
}

fun readAnswerNumber(maxOption: Int): Int? {
    print("Ваш ответ (введите номер варианта): ")
    val input = readln()
    val n = input.toIntOrNullInRange(0..maxOption)
    if (n == null) println("Введите число от 0 до $maxOption\n")
    return n
}