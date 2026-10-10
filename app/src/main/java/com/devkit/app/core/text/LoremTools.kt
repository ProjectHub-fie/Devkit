package com.devkit.app.core.text

import kotlin.random.Random

/** Lorem ipsum generation from the classic word list. */
object LoremTools {

    private val WORDS = (
        "lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt " +
            "ut labore et dolore magna aliqua enim ad minim veniam quis nostrud exercitation ullamco " +
            "laboris nisi aliquip ex ea commodo consequat duis aute irure in reprehenderit voluptate " +
            "velit esse cillum eu fugiat nulla pariatur excepteur sint occaecat cupidatat non proident " +
            "sunt culpa qui officia deserunt mollit anim id est laborum"
        ).split(" ")

    enum class Unit { PARAGRAPHS, SENTENCES, WORDS, LISTS }

    fun generate(
        unit: Unit,
        amount: Int,
        startWithLorem: Boolean = true,
        random: Random = Random.Default,
    ): String {
        val n = amount.coerceIn(1, 500)
        return when (unit) {
            Unit.PARAGRAPHS -> (1..n).joinToString("\n\n") { i ->
                generateSentences(3 + random.nextInt(4), startWithLorem && i == 1, random)
            }
            Unit.SENTENCES -> generateSentences(n, startWithLorem, random)
            Unit.WORDS -> generateWords(n, startWithLorem, random).joinToString(" ").replaceFirstChar { it.uppercase() } + "."
            Unit.LISTS -> (1..n).joinToString("\n") { i ->
                "- ${generateSentences(1, startWithLorem && i == 1, random)}"
            }
        }
    }

    private fun generateSentences(count: Int, startWithLorem: Boolean, random: Random): String {
        val sentences = mutableListOf<String>()
        for (i in 0 until count) {
            val words = generateWords(6 + random.nextInt(9), startWithLorem && i == 0, random)
            sentences.add(words.joinToString(" ").replaceFirstChar { it.uppercase() } + ".")
        }
        return sentences.joinToString(" ")
    }

    private fun generateWords(count: Int, startWithLorem: Boolean, random: Random): List<String> {
        val out = mutableListOf<String>()
        if (startWithLorem) {
            out.add("Lorem"); out.add("ipsum")
        }
        while (out.size < count) out.add(WORDS[random.nextInt(WORDS.size)])
        return out.take(count.coerceAtLeast(2))
    }
}
