import data.FileUserDictionary
import model.Statistics
import org.junit.jupiter.api.Test
import trainer.LearnWordsTrainer
import java.io.File
import kotlin.test.*

class LearnWordsTrainerTest {

    private fun createTempDictionaryFile(content: String): File =
        File.createTempFile("dictionary_", ".txt").apply {
            writeText(content.trimIndent())
            deleteOnExit()
        }

    private fun create4Of7DictionaryFile(): File = createTempDictionaryFile(
        """
        a|а|3
        b|б|3
        c|с|3
        d|д|3
        e|е|0
        f|ф|0
        g|г|0
        """
    )

    @Test
    fun `test statistics with 4 words of 7`() {
        val file = create4Of7DictionaryFile()
        val dictionary = FileUserDictionary(file.absolutePath)
        val trainer = LearnWordsTrainer(dictionary)

        assertEquals(
            Statistics(totalCount = 7, learnedCount = 4, percent = 57),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test statistics with corrupted file`() {
        val file = createTempDictionaryFile(
            """
            good|хорошо|0
            corrupted_line_without_separator
            """
        )

        assertFailsWith<IllegalArgumentException> {
            LearnWordsTrainer(FileUserDictionary(file.absolutePath))
        }
    }

    @Test
    fun `test getNextQuestion() with 5 unlearned words`() {
        val file = createTempDictionaryFile(
            """
            a|а|0
            b|б|0
            c|с|0
            d|д|0
            e|е|0
            learned|выучено|3
            """
        )

        val dictionary = FileUserDictionary(file.absolutePath)
        val trainer = LearnWordsTrainer(dictionary)

        val question = trainer.getNextQuestion()
        assertNotNull(question)
        assertTrue(question.options.isNotEmpty())
    }

    @Test
    fun `test getNextQuestion() with 1 unleard word`() {
        val file = createTempDictionaryFile(
            """
            only|только|0
            learned1|выучено|3
            learned2|выучено|3
            """
        )

        val dictionary = FileUserDictionary(file.absolutePath)
        val trainer = LearnWordsTrainer(dictionary)

        val question = trainer.getNextQuestion()
        assertNotNull(question)
        assertTrue(question.options.size in 1..4)
    }

    @Test
    fun `test getNextQuestion() with all words learned`() {
        val file = createTempDictionaryFile(
            """
            a|а|3
            b|б|3
            c|с|3
            """
        )

        val dictionary = FileUserDictionary(file.absolutePath)
        val trainer = LearnWordsTrainer(dictionary)

        val question = trainer.getNextQuestion()
        assertNull(question)
    }

    @Test
    fun `test checkAnswer() with true`() {
        val file = create4Of7DictionaryFile()
        val dictionary = FileUserDictionary(file.absolutePath)
        val trainer = LearnWordsTrainer(dictionary)

        val question = trainer.getNextQuestion()
        assertNotNull(question)

        val correctIndex = question.correctOptionIndex
        val result = trainer.checkAnswer(correctIndex)

        assertTrue(result)
    }

    @Test
    fun `test checkAnswer() with false`() {
        val file = create4Of7DictionaryFile()
        val dictionary = FileUserDictionary(file.absolutePath)
        val trainer = LearnWordsTrainer(dictionary)

        val question = trainer.getNextQuestion()
        assertNotNull(question)

        val result = trainer.checkAnswer(-1)

        assertFalse(result)
    }

    @Test
    fun `test resetProgress() with 2 words in dictionary`() {
        val file = createTempDictionaryFile(
            """
            a|а|2
            b|б|3
            """
        )

        val dictionary = FileUserDictionary(file.absolutePath)
        val trainer = LearnWordsTrainer(dictionary)

        trainer.resetProgress()

        val after = dictionary.getUnlearnedWords() + dictionary.getLearnedWords()
        assertEquals(0, after[0].correctAnswersCount)
        assertEquals(0, after[1].correctAnswersCount)
    }
}