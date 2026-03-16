package data

import trainer.Word
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant

class DictionaryDataSource(private val dbFilePath: String) {

    init {
        createSchemaIfNeeded()
    }

    fun updateDictionary(wordsFile: File): Int {
        val lines = wordsFile.readLines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return 0

        var inserted = 0

        connection().use { connection ->
            val statement = connection.prepareStatement(
                """
                INSERT OR IGNORE INTO words(text, translate)
                VALUES(?, ?)
                """.trimIndent()
            )

            for (line in lines) {
                val parts = line.split("|")
                if (parts.size < 2) continue

                val original = parts[0].trim()
                val translate = parts[1].trim()
                if (original.isBlank() || translate.isBlank()) continue

                statement.setString(1, original)
                statement.setString(2, translate)
                val affected = statement.executeUpdate()
                if (affected > 0) inserted += affected
            }
        }

        return inserted
    }

    fun ensureUser(chatId: Long, username: String? = null) {
        connection().use { connection ->
            val statement = connection.prepareStatement(
                """
                INSERT INTO users(chat_id, username, created_at)
                VALUES(?, ?, ?)
                ON CONFLICT(chat_id) DO UPDATE SET
                    username = COALESCE(excluded.username, users.username)
                """.trimIndent()
            )
            statement.setLong(1, chatId)
            statement.setString(2, username)
            statement.setString(3, Instant.now().toString())
            statement.executeUpdate()
        }
    }

    fun getSize(): Int {
        connection().use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM words").use { statement ->
                statement.executeQuery().use { rs ->
                    return if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    fun getNumOfLearnedWords(chatId: Long, learningThreshold: Int): Int {
        connection().use { connection ->
            val statement = connection.prepareStatement(
                """
                SELECT COUNT(*)
                FROM words w
                LEFT JOIN user_answers ua
                    ON ua.word_id = w.id
                    AND ua.user_id = (SELECT id FROM users WHERE chat_id = ?)
                WHERE COALESCE(ua.correct_answer_count, 0) >= ?
                """.trimIndent()
            )
            statement.setLong(1, chatId)
            statement.setInt(2, learningThreshold)
            statement.executeQuery().use { rs ->
                return if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    fun getLearnedWords(chatId: Long, learningThreshold: Int): List<Word> =
        getWordsByThreshold(chatId, learningThreshold, learned = true)

    fun getUnlearnedWords(chatId: Long, learningThreshold: Int): List<Word> =
        getWordsByThreshold(chatId, learningThreshold, learned = false)

    fun setCorrectAnswersCount(chatId: Long, word: String, correctAnswersCount: Int) {
        connection().use { connection ->
            val userId = getUserId(connection, chatId) ?: return
            val wordId = getWordId(connection, word) ?: return

            val statement = connection.prepareStatement(
                """
                INSERT INTO user_answers(user_id, word_id, correct_answer_count, updated_at)
                VALUES(?, ?, ?, ?)
                ON CONFLICT(user_id, word_id) DO UPDATE SET
                    correct_answer_count = excluded.correct_answer_count,
                    updated_at = excluded.updated_at
                """.trimIndent()
            )

            statement.setLong(1, userId)
            statement.setLong(2, wordId)
            statement.setInt(3, correctAnswersCount.coerceAtLeast(0))
            statement.setString(4, Instant.now().toString())
            statement.executeUpdate()
        }
    }

    fun resetUserProgress(chatId: Long) {
        connection().use { connection ->
            val userId = getUserId(connection, chatId) ?: return

            connection.prepareStatement(
                "DELETE FROM user_answers WHERE user_id = ?"
            ).use { statement ->
                statement.setLong(1, userId)
                statement.executeUpdate()
            }
        }
    }

    fun getCorrectAnswersCount(chatId: Long, word: String): Int {
        connection().use { connection ->
            val statement = connection.prepareStatement(
                """
                SELECT COALESCE(ua.correct_answer_count, 0)
                FROM words w
                LEFT JOIN user_answers ua
                    ON ua.word_id = w.id
                    AND ua.user_id = (SELECT id FROM users WHERE chat_id = ?)
                WHERE w.text = ?
                """.trimIndent()
            )
            statement.setLong(1, chatId)
            statement.setString(2, word)
            statement.executeQuery().use { rs ->
                return if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    private fun getWordsByThreshold(chatId: Long, learningThreshold: Int, learned: Boolean): List<Word> {
        val comparator = if (learned) ">=" else "<"

        connection().use { connection ->
            val statement = connection.prepareStatement(
                """
                SELECT w.text, w.translate, COALESCE(ua.correct_answer_count, 0) AS correct_answer_count
                FROM words w
                LEFT JOIN user_answers ua
                    ON ua.word_id = w.id
                    AND ua.user_id = (SELECT id FROM users WHERE chat_id = ?)
                WHERE COALESCE(ua.correct_answer_count, 0) $comparator ?
                ORDER BY w.id
                """.trimIndent()
            )
            statement.setLong(1, chatId)
            statement.setInt(2, learningThreshold)

            statement.executeQuery().use { rs ->
                val result = mutableListOf<Word>()
                while (rs.next()) {
                    result.add(
                        Word(
                            original = rs.getString("text"),
                            translate = rs.getString("translate"),
                            correctAnswersCount = rs.getInt("correct_answer_count")
                        )
                    )
                }
                return result
            }
        }
    }

    private fun createSchemaIfNeeded() {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("PRAGMA foreign_keys = ON")
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT,
                        created_at TEXT NOT NULL,
                        chat_id INTEGER NOT NULL UNIQUE
                    )
                    """.trimIndent()
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS words (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        text TEXT NOT NULL UNIQUE,
                        translate TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS user_answers (
                        user_id INTEGER NOT NULL,
                        word_id INTEGER NOT NULL,
                        correct_answer_count INTEGER NOT NULL DEFAULT 0,
                        updated_at TEXT NOT NULL,
                        PRIMARY KEY(user_id, word_id),
                        FOREIGN KEY(user_id) REFERENCES users(id),
                        FOREIGN KEY(word_id) REFERENCES words(id)
                    )
                    """.trimIndent()
                )
            }
        }
    }

    private fun getUserId(connection: Connection, chatId: Long): Long? {
        connection.prepareStatement("SELECT id FROM users WHERE chat_id = ?").use { statement ->
            statement.setLong(1, chatId)
            statement.executeQuery().use { rs ->
                if (rs.next()) return rs.getLong(1)
            }
        }
        return null
    }

    private fun getWordId(connection: Connection, word: String): Long? {
        connection.prepareStatement("SELECT id FROM words WHERE text = ?").use { statement ->
            statement.setString(1, word)
            statement.executeQuery().use { rs ->
                if (rs.next()) return rs.getLong(1)
            }
        }
        return null
    }

    private fun connection(): Connection = DriverManager.getConnection("jdbc:sqlite:$dbFilePath")
}

