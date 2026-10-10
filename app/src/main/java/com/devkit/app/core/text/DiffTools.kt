package com.devkit.app.core.text

/**
 * Line based LCS diff. Inputs are capped so pathological sizes cannot exhaust
 * memory on low-end devices; callers are told when truncation happened.
 */
object DiffTools {

    data class DiffLine(val type: Type, val text: String, val leftNo: Int?, val rightNo: Int?) {
        enum class Type { SAME, ADDED, REMOVED }
    }

    data class DiffResult(val lines: List<DiffLine>, val truncated: Boolean, val added: Int, val removed: Int)

    const val MAX_LINES = 4000

    fun diff(left: String, right: String): DiffResult {
        var truncated = false
        var a = left.split("\n")
        var b = right.split("\n")
        if (a.size > MAX_LINES) { a = a.take(MAX_LINES); truncated = true }
        if (b.size > MAX_LINES) { b = b.take(MAX_LINES); truncated = true }

        val lcs = lcsTable(a, b)
        val out = mutableListOf<DiffLine>()
        var i = 0
        var j = 0
        var added = 0
        var removed = 0
        while (i < a.size && j < b.size) {
            when {
                a[i] == b[j] -> {
                    out.add(DiffLine(DiffLine.Type.SAME, a[i], i + 1, j + 1))
                    i++; j++
                }
                lcs[i + 1][j] >= lcs[i][j + 1] -> {
                    out.add(DiffLine(DiffLine.Type.REMOVED, a[i], i + 1, null))
                    removed++; i++
                }
                else -> {
                    out.add(DiffLine(DiffLine.Type.ADDED, b[j], null, j + 1))
                    added++; j++
                }
            }
        }
        while (i < a.size) { out.add(DiffLine(DiffLine.Type.REMOVED, a[i], i + 1, null)); removed++; i++ }
        while (j < b.size) { out.add(DiffLine(DiffLine.Type.ADDED, b[j], null, j + 1)); added++; j++ }
        return DiffResult(out, truncated, added, removed)
    }

    private fun lcsTable(a: List<String>, b: List<String>): Array<IntArray> {
        val table = Array(a.size + 1) { IntArray(b.size + 1) }
        for (x in a.size - 1 downTo 0) {
            for (y in b.size - 1 downTo 0) {
                table[x][y] = if (a[x] == b[y]) table[x + 1][y + 1] + 1
                else maxOf(table[x + 1][y], table[x][y + 1])
            }
        }
        return table
    }
}
