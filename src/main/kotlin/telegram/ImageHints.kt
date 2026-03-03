package telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties

@Serializable
data class ImageHintEntry(
    val path: String,
    @SerialName("fileId") val fileId: String? = null,
    val hasSpoiler: Boolean = false
)

/**
 * Формат image_index.json:
 * {
 *   "cat": { "path": "images/cat.jpg", "fileId": null, "hasSpoiler": false },
 *   "dog": { "path": "images/dog.jpg", "fileId": null, "hasSpoiler": true }
 * }
 */
class ImageIndex(private val map: Map<String, ImageHintEntry>) {
    val size: Int get() = map.size

    fun find(word: String): ImageHintEntry? = map[word.trim().lowercase()]

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val mapSerializer = MapSerializer(String.serializer(), ImageHintEntry.serializer())

        fun loadFromResources(resourceName: String = "image_index.json"): ImageIndex {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourceName)
            if (stream == null) {
                println("DEBUG: image index resource not found: $resourceName")
                return ImageIndex(emptyMap())
            }
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsed: Map<String, ImageHintEntry> = json.decodeFromString(mapSerializer, text)
            return ImageIndex(parsed)
        }
    }
}

/** Кэш word -> file_id. Храним рядом с jar/в папке запуска. */
class ImageFileIdCache(private val file: File) {
    private val props = Properties()

    init {
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
        }
    }

    fun get(wordKey: String): String? = props.getProperty(wordKey)

    fun put(wordKey: String, fileId: String) {
        props.setProperty(wordKey, fileId)
        file.parentFile?.mkdirs()
        file.outputStream().use { out -> props.store(out, "telegram image file_id cache") }
    }
}

/**
 * Достаёт картинку из resources и кладёт её на диск, чтобы можно было отправить в multipart.
 * Работает как при запуске из IDE, так и из jar.
 */
object ResourceFileExtractor {
    fun extractTo(
        resourcePath: String,
        targetDir: File = File("runtime_images")
    ): File {
        val normalized = resourcePath.removePrefix("/")
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(normalized)
            ?: error("Resource not found: $normalized")

        val outFile = File(targetDir, File(normalized).name)
        if (outFile.exists() && outFile.length() > 0) return outFile

        targetDir.mkdirs()
        stream.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
        return outFile
    }
}
