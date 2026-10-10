package com.devkit.app.core.web

import com.devkit.app.core.ToolResult

/** Identifies likely hash algorithms by length/charset and compares two hashes. */
object HashInspector {

    data class Candidate(val algorithm: String, val hexLength: Int)

    private val CANDIDATES = listOf(
        Candidate("MD5", 32),
        Candidate("SHA-1", 40),
        Candidate("SHA-224", 56),
        Candidate("SHA-256", 64),
        Candidate("SHA-384", 96),
        Candidate("SHA-512", 128),
        Candidate("CRC-32 (hex)", 8),
    )

    fun identify(hash: String): ToolResult<List<Candidate>> {
        val cleaned = hash.trim().replace(Regex("\\s"), "")
        if (cleaned.isEmpty()) return ToolResult.Failure("Paste a hash first.")
        if (!cleaned.matches(Regex("[0-9a-fA-F]+"))) {
            return ToolResult.Failure("A hex digest only contains the characters 0-9 and a-f.")
        }
        val matches = CANDIDATES.filter { it.hexLength == cleaned.length }
        if (matches.isEmpty()) {
            return ToolResult.Failure("No common algorithm produces a ${cleaned.length}-character hex digest.")
        }
        return ToolResult.Success(matches)
    }

    /** Constant-time comparison to avoid timing leaks when comparing secrets. */
    fun compare(a: String, b: String): Boolean {
        val x = a.trim().lowercase().toByteArray()
        val y = b.trim().lowercase().toByteArray()
        if (x.size != y.size) return false
        var diff = 0
        for (i in x.indices) diff = diff or (x[i].toInt() xor y[i].toInt())
        return diff == 0
    }
}
