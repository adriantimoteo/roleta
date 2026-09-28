package com.roleta.app.data.repository

/** Pasted text split into a list title and its items. */
data class ParsedBulkImport(
    val title: String,
    /** Unique items, in the order they were pasted. */
    val items: List<String>,
    /** Lines dropped because they repeat an earlier item (case-insensitive). */
    val duplicates: List<String>
)

object BulkImportParser {

    // Common list markers left over when copying from notes apps ("- Pizza", "• Pizza").
    private val BULLET_PREFIX = Regex("""^[-*•]\s+""")

    /**
     * The first non-blank line is the title; each following non-blank line is an item.
     * Lines are trimmed, blank lines are ignored, and repeated items go to [ParsedBulkImport.duplicates].
     * Returns null when there is no title.
     */
    fun parse(text: String): ParsedBulkImport? {
        val lines = text.lines()
            .map { it.trim().replace(BULLET_PREFIX, "").trim() }
            .filter { it.isNotEmpty() }
        val title = lines.firstOrNull() ?: return null

        val seen = HashSet<String>()
        val items = mutableListOf<String>()
        val duplicates = mutableListOf<String>()
        for (line in lines.drop(1)) {
            if (seen.add(line.lowercase())) items += line else duplicates += line
        }
        return ParsedBulkImport(title, items, duplicates)
    }
}
