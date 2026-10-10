package com.devkit.app.core.text

/** Text analysis helpers. Counting rules are documented here and surfaced in the UI. */
object TextStats {

    data class Counts(
        val characters: Int,
        val charactersNoSpaces: Int,
        val words: Int,
        val lines: Int,
        val paragraphs: Int,
        val sentences: Int,
    )

    /**
     * Counts using these rules:
     * - characters: total UTF-16 code units (what an editor reports).
     * - words: maximal runs of non-whitespace.
     * - lines: number of newline separated lines; empty input is 0.
     * - paragraphs: blank-line separated blocks.
     * - sentences: runs terminated by . ! ? or end of text.
     */
    fun analyze(input: String): Counts {
        val characters = input.length
        val charactersNoSpaces = input.count { !it.isWhitespace() }
        val words = if (input.isBlank()) 0 else Regex("\\S+").findAll(input).count()
        val lines = if (input.isEmpty()) 0 else input.split("\n").size
        val paragraphs = input.split(Regex("(\\r?\\n\\s*\\r?\\n)+"))
            .count { it.isNotBlank() }
        val sentences = Regex("[^.!?\\n]*[.!?]+|[^.!?\\n]+$")
            .findAll(input).count { it.value.isNotBlank() }
        return Counts(characters, charactersNoSpaces, words, lines, paragraphs, sentences)
    }

    fun wordFrequency(input: String, limit: Int = 10): List<Pair<String, Int>> =
        Regex("[A-Za-z0-9']+").findAll(input.lowercase())
            .map { it.value }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { it.key to it.value }
}

/** Case conversion helpers for the case converter tool. */
object CaseConverter {

    private fun words(input: String): List<String> =
        Regex("[A-Za-z0-9]+").findAll(input)
            .map { it.value }
            .toList()

    fun upper(input: String) = input.uppercase()
    fun lower(input: String) = input.lowercase()

    fun title(input: String) = words(input).joinToString(" ") { w ->
        w.lowercase().replaceFirstChar { it.uppercase() }
    }

    fun sentence(input: String): String {
        if (input.isEmpty()) return input
        val lowered = input.lowercase()
        return lowered.replaceFirstChar { it.uppercase() }
    }

    fun camel(input: String): String = words(input).mapIndexed { index, w ->
        if (index == 0) w.lowercase() else w.lowercase().replaceFirstChar { it.uppercase() }
    }.joinToString("")

    fun pascal(input: String): String = words(input)
        .joinToString("") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

    fun snake(input: String) = words(input).joinToString("_") { it.lowercase() }
    fun kebab(input: String) = words(input).joinToString("-") { it.lowercase() }

    fun constant(input: String) = words(input).joinToString("_") { it.uppercase() }
}
