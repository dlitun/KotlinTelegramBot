package model

import kotlinx.serialization.Serializable

@Serializable
data class ImageHint(
    val path: String,
    val fileId: String? = null,
    val hasSpoiler: Boolean = false
)