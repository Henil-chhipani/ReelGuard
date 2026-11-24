package com.reelguard.app

import java.util.Locale

/**
 * Matches Instagram comment sheets from their visible labels. A sheet needs a
 * title and a composer hint, so an inline "Add a comment" on a feed post is not
 * mistaken for an open comment sheet.
 */
internal object SheetMatcher {

    private const val COUNT = "[\\d.,]+\\s*[km]?"

    private val titleWithCount = Regex("^comments?\\s*[·•(]?\\s*$COUNT\\s*\\)?$")
    private val countWithTitle = Regex("^$COUNT\\s+comments?$")

    private val composerPrefixes = listOf(
        "add a comment",
        "be the first to comment",
        "start the conversation",
    )

    fun isCommentsTitle(raw: String): Boolean {
        val text = normalize(raw)
        return text == "comments" ||
            text == "comment" ||
            titleWithCount.matches(text) ||
            countWithTitle.matches(text)
    }

    fun isCommentsComposer(raw: String): Boolean {
        val text = normalize(raw)
        if (text.isEmpty()) return false
        if (text.startsWith("view all") && text.contains("comment")) return true
        return composerPrefixes.any { text.startsWith(it) }
    }

    fun hasCommentsSheet(labels: List<String>): Boolean =
        labels.any(::isCommentsTitle) && labels.any(::isCommentsComposer)

    private fun normalize(raw: String): String = raw.trim().lowercase(Locale.US)
}
