package com.devkit.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs CPU-heavy tool work away from the main thread. Every tool ViewModel uses
 * this so parsing large JSON or diffing long texts never janks the UI.
 */
suspend fun <T> compute(block: () -> T): T = withContext(Dispatchers.Default) { block() }

/**
 * Applies `{name}` style environment substitutions to a request field.
 * Values come from a user-defined environment; unresolved names are reported.
 */
object VariableSubstitution {
    private val PATTERN = Regex("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}")

    data class Result(val output: String, val unresolved: List<String>)

    fun apply(input: String, variables: Map<String, String>): Result {
        val unresolved = mutableListOf<String>()
        val output = PATTERN.replace(input) { match ->
            val key = match.groupValues[1]
            val value = variables[key]
            if (value == null) {
                unresolved.add(key)
                match.value
            } else value
        }
        return Result(output, unresolved.distinct())
    }

    fun applyAll(inputs: List<String>, variables: Map<String, String>): Result {
        val unresolved = mutableListOf<String>()
        val outputs = inputs.map { input ->
            val r = apply(input, variables)
            unresolved.addAll(r.unresolved)
            r.output
        }
        return Result(outputs.joinToString("\u0000"), unresolved.distinct())
    }
}
