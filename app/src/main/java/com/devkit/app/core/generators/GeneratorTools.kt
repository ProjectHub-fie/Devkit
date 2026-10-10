package com.devkit.app.core.generators

import com.devkit.app.core.ToolResult
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object GeneratorTools {

    private val secureRandom = SecureRandom()

    // ---------- UUID ----------

    enum class UuidVersion { V4_RANDOM, V7_TIME_ORDERED }

    /**
     * V4 uses UUID.randomUUID (backed by SecureRandom). V7 is constructed from
     * the current time plus randomness, following RFC 9562 layout, which is
     * useful as a sortable identifier.
     */
    fun uuid(version: UuidVersion = UuidVersion.V4_RANDOM): String = when (version) {
        UuidVersion.V4_RANDOM -> UUID.randomUUID().toString()
        UuidVersion.V7_TIME_ORDERED -> uuidV7()
    }

    fun uuids(count: Int, version: UuidVersion = UuidVersion.V4_RANDOM): ToolResult<List<String>> {
        if (count !in 1..1000) return ToolResult.Failure("Choose between 1 and 1000 UUIDs.")
        return ToolResult.Success(List(count) { uuid(version) })
    }

    fun uuidV7(): String {
        val millis = System.currentTimeMillis()
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        bytes[0] = ((millis shr 40) and 0xFF).toByte()
        bytes[1] = ((millis shr 32) and 0xFF).toByte()
        bytes[2] = ((millis shr 24) and 0xFF).toByte()
        bytes[3] = ((millis shr 16) and 0xFF).toByte()
        bytes[4] = ((millis shr 8) and 0xFF).toByte()
        bytes[5] = (millis and 0xFF).toByte()
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte() // version 7
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte() // variant
        return formatUuid(bytes)
    }

    private fun formatUuid(bytes: ByteArray): String {
        val sb = StringBuilder(36)
        for (i in bytes.indices) {
            if (i == 4 || i == 6 || i == 8 || i == 10) sb.append('-')
            sb.append("%02x".format(bytes[i].toInt() and 0xFF))
        }
        return sb.toString()
    }

    // ---------- Random strings / passwords ----------

    const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val DIGITS = "0123456789"
    const val SYMBOLS = "!@#\$%^&*()-_=+[]{};:,.?/"

    /**
     * Random string without ambiguity requirements. Uses SecureRandom so the
     * result is not predictable, but see [password] for guaranteed-inclusion
     * password generation.
     */
    fun randomString(
        length: Int,
        alphabet: String,
        excludeAmbiguous: Boolean = false,
    ): ToolResult<String> {
        if (length !in 1..10_000) return ToolResult.Failure("Length must be between 1 and 10000.")
        val pool = buildString {
            append(alphabet)
        }.let { if (excludeAmbiguous) it.filterNot { c -> c in "Il1O0o" } else it }
        if (pool.isEmpty()) return ToolResult.Failure("Select at least one character set.")
        return ToolResult.Success(
            buildString { repeat(length) { append(pool[secureRandom.nextInt(pool.length)]) } }
        )
    }

    /** Guarantees one character from each requested set, then fills and shuffles. */
    fun password(
        length: Int,
        useLower: Boolean,
        useUpper: Boolean,
        useDigits: Boolean,
        useSymbols: Boolean,
    ): ToolResult<String> {
        val sets = buildList {
            if (useLower) add(LOWER)
            if (useUpper) add(UPPER)
            if (useDigits) add(DIGITS)
            if (useSymbols) add(SYMBOLS)
        }
        if (sets.isEmpty()) return ToolResult.Failure("Select at least one character set.")
        if (length < sets.size) return ToolResult.Failure("Length must be at least ${sets.size} for the selected sets.")
        if (length > 10_000) return ToolResult.Failure("Length must be at most 10000.")
        val chars = mutableListOf<Char>()
        sets.forEach { chars.add(it[secureRandom.nextInt(it.length)]) }
        val pool = sets.joinToString("")
        while (chars.size < length) chars.add(pool[secureRandom.nextInt(pool.length)])
        for (i in chars.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp
        }
        return ToolResult.Success(chars.joinToString(""))
    }

    fun passwordStrength(password: String): Strength {
        var score = 0
        if (password.length >= 12) score += 2 else if (password.length >= 8) score += 1
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        val entropy = password.length * kotlin.math.log2(
            (guessAlphabetSize(password)).coerceAtLeast(2).toDouble()
        )
        val label = when {
            score <= 2 -> "Weak"
            score <= 4 -> "Fair"
            score <= 5 -> "Good"
            else -> "Strong"
        }
        return Strength(score.coerceIn(0, 6), entropy, label)
    }

    private fun guessAlphabetSize(p: String): Int {
        var size = 0
        if (p.any { it.isLowerCase() }) size += 26
        if (p.any { it.isUpperCase() }) size += 26
        if (p.any { it.isDigit() }) size += 10
        if (p.any { !it.isLetterOrDigit() }) size += 20
        return size
    }

    data class Strength(val score: Int, val entropyBits: Double, val label: String)

    // ---------- Hashing ----------

    enum class HashAlgorithm(val id: String) {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256"),
        SHA384("SHA-384"),
        SHA512("SHA-512");
    }

    fun hash(input: String, algorithm: HashAlgorithm): ToolResult<String> = try {
        val digest = MessageDigest.getInstance(algorithm.id)
        ToolResult.Success(digest.digest(input.toByteArray(Charsets.UTF_8)).toHex())
    } catch (e: Exception) {
        ToolResult.Failure("Could not compute hash: ${e.message}")
    }

    fun hmac(input: String, key: String, algorithm: HashAlgorithm): ToolResult<String> = try {
        val macName = when (algorithm) {
            HashAlgorithm.MD5 -> "HmacMD5"
            HashAlgorithm.SHA1 -> "HmacSHA1"
            HashAlgorithm.SHA256 -> "HmacSHA256"
            HashAlgorithm.SHA384 -> "HmacSHA384"
            HashAlgorithm.SHA512 -> "HmacSHA512"
        }
        val mac = javax.crypto.Mac.getInstance(macName)
        mac.init(javax.crypto.spec.SecretKeySpec(key.toByteArray(Charsets.UTF_8), macName))
        ToolResult.Success(mac.doFinal(input.toByteArray(Charsets.UTF_8)).toHex())
    } catch (e: Exception) {
        ToolResult.Failure("Could not compute HMAC: ${e.message}")
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}
