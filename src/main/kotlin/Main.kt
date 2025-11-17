package org.example

import java.io.File

fun main() {
    val file = File("src/main/resources/words.txt")
    val lines = file.readLines()

    for (line in lines) {
        println(line)
    }
}