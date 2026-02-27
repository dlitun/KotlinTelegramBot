package data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import model.ImageHint
import java.io.File

class ImageHintsRepository(
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true },
    private val indexFile: File = File("src/main/resources/image_index.json")
) {

    fun load(): MutableMap<String, ImageHint> {
        if (!indexFile.exists()) return mutableMapOf()
        return indexFile.inputStream().use { input ->
            json.decodeFromStream<Map<String, ImageHint>>(input).toMutableMap()
        }
    }

    fun save(map: Map<String, ImageHint>) {
        if (!indexFile.exists()) {
            indexFile.parentFile?.mkdirs()
            indexFile.createNewFile()
        }
        indexFile.writeText(json.encodeToString(map))
    }
}