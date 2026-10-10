package com.devkit.app.core

/**
 * Outcome of a pure tool operation. Tools never throw into the UI layer; they
 * return a [Failure] with a user-presentable message instead.
 */
sealed interface ToolResult<out T> {
    data class Success<T>(val value: T) : ToolResult<T>
    data class Failure(val message: String) : ToolResult<Nothing>

    fun getOrNull(): T? = (this as? Success)?.value
    fun errorOrNull(): String? = (this as? Failure)?.message
}

inline fun <T, R> ToolResult<T>.map(transform: (T) -> R): ToolResult<R> = when (this) {
    is ToolResult.Success -> ToolResult.Success(transform(value))
    is ToolResult.Failure -> this
}

fun <T> runTool(block: () -> T): ToolResult<T> = try {
    ToolResult.Success(block())
} catch (t: Throwable) {
    ToolResult.Failure(t.message ?: t::class.java.simpleName)
}
