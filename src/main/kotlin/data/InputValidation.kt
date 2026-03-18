package data

import security.InputSecurity

private val safeWordRegex = Regex("""^[\p{L}\p{N}\s'\-]{1,80}$""")
private val safeUsernameRegex = Regex("""^[A-Za-z0-9_\-.]{1,64}$""")

internal fun validateDictionaryWord(value: String, source: String): String? {
    val normalized = value.trim()
    if (normalized.isBlank()) return null

    if (InputSecurity.containsSuspiciousSqlPattern(normalized)) {
        InputSecurity.logSuspiciousInput(source, normalized)
    }

    if (!safeWordRegex.matches(normalized)) {
        InputSecurity.logSuspiciousInput("$source.invalid_chars", normalized)
        return null
    }

    return normalized
}

internal fun validateUsername(value: String?): String? {
    val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return null

    if (InputSecurity.containsSuspiciousSqlPattern(normalized)) {
        InputSecurity.logSuspiciousInput("username", normalized)
    }

    if (!safeUsernameRegex.matches(normalized)) {
        InputSecurity.logSuspiciousInput("username.invalid_chars", normalized)
        return null
    }

    return normalized
}
