package data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import model.ImageHint
import java.io.File

class ImageHintsRepository(
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true },
    private val baseIndexResourcePath: String = "image_index.json",
    private val cacheFile: File = File("users", "image_index_cache.json")
) {

    fun load(): MutableMap<String, ImageHint> {
        val baseIndex: MutableMap<String, ImageHint> = loadBaseIndexFromResources().toMutableMap()
        val cached = loadCacheFromFile()

        // merge: base defines path/hasSpoiler, cache overrides only fileId
        cached.forEach { (word, cachedHint) ->
            val base = baseIndex[word]
            if (base != null) {
                baseIndex[word] = base.copy(fileId = cachedHint.fileId ?: base.fileId)
            } else {
                // allow cache to extend index, just in case
                baseIndex[word] = cachedHint
            }
        }

        return baseIndex
    }

    fun save(map: Map<String, ImageHint>) {
        // сохраняем только fileId, потому что resources внутри jar неизменяемы
        val onlyIds = map
            .mapValues { (_, hint) -> ImageHint(path = hint.path, fileId = hint.fileId, hasSpoiler = hint.hasSpoiler) }
            .filterValues { it.fileId != null }

        if (!cacheFile.exists()) {
            cacheFile.parentFile?.mkdirs()
            cacheFile.createNewFile()
        }
        cacheFile.writeText(json.encodeToString(onlyIds))
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadBaseIndexFromResources(): Map<String, ImageHint> {
        val stream = this::class.java.classLoader.getResourceAsStream(baseIndexResourcePath)
            ?: return emptyMap()

        return stream.use { input ->
            json.decodeFromStream<Map<String, ImageHint>>(input)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadCacheFromFile(): Map<String, ImageHint> {
        if (!cacheFile.exists()) return emptyMap()
        return cacheFile.inputStream().use { input ->
            json.decodeFromStream<Map<String, ImageHint>>(input)
        }
    }
}