import data.DictionaryDataSource
import data.validateUsername
import org.junit.jupiter.api.Test
import security.InputSecurity
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DictionaryDataSourceSqlInjectionTest {

    private fun createDbFile(): File =
        kotlin.io.path.createTempFile(prefix = "trainer_sql_injection_", suffix = ".db").toFile().apply {
            deleteOnExit()
        }

    private fun createDictionaryFile(content: String): File =
        File.createTempFile("dictionary_sql_injection_", ".txt").apply {
            writeText(content.trimIndent())
            deleteOnExit()
        }

    @Test
    fun `should not execute injected SQL from dictionary upload`() {
        val dbFile = createDbFile()
        val dataSource = DictionaryDataSource(dbFile.absolutePath)

        val wordsFile = createDictionaryFile(
            """
            hello|привет
            bad'; DROP TABLE words; --|вред
            """
        )

        val inserted = dataSource.updateDictionary(wordsFile)

        assertEquals(1, inserted)
        assertEquals(1, dataSource.getSize())
    }

    @Test
    fun `should not update any other word when payload is used as word key`() {
        val dbFile = createDbFile()
        val dataSource = DictionaryDataSource(dbFile.absolutePath)
        val chatId = 400164658L

        val wordsFile = createDictionaryFile(
            """
            hello|привет
            world|мир
            """
        )

        dataSource.updateDictionary(wordsFile)
        dataSource.ensureUser(chatId)

        dataSource.setCorrectAnswersCount(chatId, "hello", 2)
        dataSource.setCorrectAnswersCount(chatId, "hello' OR '1'='1", 99)

        assertEquals(2, dataSource.getCorrectAnswersCount(chatId, "hello"))
        assertEquals(0, dataSource.getCorrectAnswersCount(chatId, "world"))
    }

    @Test
    fun `should detect suspicious SQL payload patterns`() {
        assertTrue(InputSecurity.containsSuspiciousSqlPattern("admin' OR 1=1 --"))
        assertFalse(InputSecurity.containsSuspiciousSqlPattern("english-trainer-bot"))
    }

    @Test
    fun `should reject unsafe username`() {
        assertNull(validateUsername("root; DROP TABLE users"))
    }
}
