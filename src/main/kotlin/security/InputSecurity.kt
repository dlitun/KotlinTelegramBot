package security

object InputSecurity {
    private val suspiciousSqlRegex = Regex(
        pattern = """(?i)(--|/\*|\*/|;|\bunion\b|\bselect\b|\bdrop\b|\bdelete\b|\binsert\b|\bupdate\b|\bor\s+1\s*=\s*1)"""
    )

    fun containsSuspiciousSqlPattern(value: String): Boolean {
        return suspiciousSqlRegex.containsMatchIn(value)
    }

    fun logSuspiciousInput(source: String, rawInput: String, chatId: Long? = null) {
        val compact = rawInput
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .take(160)

        val chatPart = if (chatId != null) " chatId=$chatId" else ""
        println("WARN: suspicious input source=$source$chatPart value='$compact'")
    }
}
