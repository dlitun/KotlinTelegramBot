package tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class Entry(val path: String, val fileId: String? = null, val hasSpoiler: Boolean = false)

fun main() {
    val projectDir = File(".").absoluteFile
    val imagesDir = File(projectDir, "src/main/resources/images")
    val outFile = File(projectDir, "src/main/resources/image_index.json")

    require(imagesDir.exists()) { "Images dir not found: ${imagesDir.absolutePath}" }

    val existing: Map<String, Entry> = if (outFile.exists()) {
        try {
            Json { ignoreUnknownKeys = true }.decodeFromString(outFile.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            emptyMap()
        }
    } else emptyMap()

    val files = imagesDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()

    val map = linkedMapOf<String, Entry>()
    for (f in files) {
        val key = f.nameWithoutExtension.trim().lowercase()
        val spoiler = existing[key]?.hasSpoiler ?: (key == "dog")
        map[key] = Entry(path = "images/${f.name}", fileId = null, hasSpoiler = spoiler)
    }

    val json = Json { prettyPrint = true }.encodeToString(map)
    outFile.writeText(json, Charsets.UTF_8)

    println("Generated ${map.size} entries -> ${outFile.absolutePath}")
}

