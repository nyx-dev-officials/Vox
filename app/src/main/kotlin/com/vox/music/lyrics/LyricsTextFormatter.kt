/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Plain-text views of a lyric blob: timestamp-free lines for the carousel
 * exporter, and a copy-friendly version with [Verse] / [Chorus] headers.
 *
 * The header pass is a heuristic, not a chart parser: existing section tags are
 * preserved untouched, and otherwise each stanza becomes "Verse n", with an
 * exact repeat of an earlier stanza labelled "Chorus". That is the useful
 * answer for the common verse/chorus/verse shape without inventing structure
 * the file does not contain.
 */

package com.vox.music.lyrics

object LyricsTextFormatter {

    private val timestamp = Regex("""^\s*\[\d{1,2}:\d{2}(?:[.:]\d{1,3})?]\s*""")
    private val sectionTag = Regex("""^\[[^\]]+]$""")

    /** Timestamp-free, non-blank lines — what a carousel slide should quote. */
    fun plainLines(lyrics: String): List<String> = lyrics
        .lineSequence()
        .map { it.replace(timestamp, "").trim() }
        .filter { it.isNotEmpty() }
        .toList()

    fun stanzas(lyrics: String): List<List<String>> {
        val result = mutableListOf<MutableList<String>>()
        result.add(mutableListOf())
        lyrics.lineSequence().forEach { raw ->
            val line = raw.replace(timestamp, "").trim()
            if (line.isEmpty()) {
                if (result.last().isNotEmpty()) result.add(mutableListOf())
            } else {
                result.last().add(line)
            }
        }
        return result.filter { it.isNotEmpty() }
    }

    /**
     * Clean plain text with section headers. A file that already carries its own
     * [Verse]/[Chorus] tags is returned with timestamps stripped and tags intact.
     */
    fun withSectionHeaders(lyrics: String): String {
        val blocks = stanzas(lyrics)
        if (blocks.isEmpty()) return ""

        val alreadyTagged = blocks.any { block -> block.firstOrNull()?.matches(sectionTag) == true }
        if (alreadyTagged) return blocks.joinToString("\n\n") { it.joinToString("\n") }

        val firstSeen = HashMap<String, Int>()
        var verse = 0
        val labelled = blocks.map { block ->
            val key = block.joinToString("\n").lowercase()
            val repeat = firstSeen.containsKey(key)
            if (!repeat) {
                firstSeen[key] = verse
                verse++
            }
            val label = if (repeat) "Chorus" else "Verse $verse"
            "[$label]\n" + block.joinToString("\n")
        }
        return labelled.joinToString("\n\n")
    }
}
