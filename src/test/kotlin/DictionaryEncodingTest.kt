import org.junit.jupiter.api.Test
import telegram.readDictionaryLinesWithFallback
import java.io.File
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DictionaryEncodingTest {

    @Test
    fun `readDictionaryLinesWithFallback reads cp1251 dictionary without mojibake`() {
        val tempFile = File.createTempFile("dictionary_cp1251_", ".txt")
        tempFile.deleteOnExit()

        val content = "hello|привет|0\nfriend|друг|0\n"
        tempFile.writeBytes(content.toByteArray(Charset.forName("windows-1251")))

        val parsed = readDictionaryLinesWithFallback(tempFile)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        assertEquals("hello|привет|0", parsed[0])
        assertEquals("friend|друг|0", parsed[1])
        assertTrue(parsed.all { !it.contains('\uFFFD') })
    }
}

