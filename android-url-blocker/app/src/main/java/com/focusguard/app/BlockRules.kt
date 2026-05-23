package com.focusguard.app

/** Compiles wildcard patterns into regexes and matches text against them. */
class BlockRules(patterns: List<String>) {

    private val regexes: List<Regex> = patterns.mapNotNull { compile(it) }

    fun matches(text: String): Boolean {
        if (text.isBlank()) return false
        return regexes.any { it.containsMatchIn(text) }
    }

    private fun compile(pattern: String): Regex? {
        val p = pattern.trim()
        if (p.isEmpty()) return null
        val sb = StringBuilder()
        for (c in p) {
            when (c) {
                '*' -> sb.append(".*")
                else -> if (c.isLetterOrDigit()) sb.append(c) else sb.append(Regex.escape(c.toString()))
            }
        }
        return try {
            Regex(sb.toString(), RegexOption.IGNORE_CASE)
        } catch (e: Exception) {
            null
        }
    }
}
