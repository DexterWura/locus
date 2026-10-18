package com.whereami.locus.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordCrypto {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    private val secureRandom = SecureRandom()

    private val recoveryWords = listOf(
        "amber", "basin", "cedar", "delta", "ember", "fjord", "grove", "haven",
        "ivory", "jade", "kelp", "lunar", "maple", "nova", "olive", "pine",
        "quartz", "ridge", "sage", "tide", "umbra", "vale", "willow", "zenith",
        "anchor", "breeze", "canyon", "dusk", "echo", "flint", "glacier", "harbor",
        "island", "jasper", "kite", "lagoon", "meadow", "north", "orbit", "pebble",
        "quill", "river", "stone", "trail", "ultra", "vista", "wave", "coral"
    )

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun hashSecret(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(secret: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val actual = hashSecret(secret, salt)
        return MessageDigest.isEqual(actual, expectedHash)
    }

    fun normalizeAnswer(answer: String): String =
        answer.trim().lowercase().replace(Regex("\\s+"), " ")

    fun generateRecoveryPhrase(wordCount: Int = 8): String {
        return (0 until wordCount).joinToString(" ") {
            recoveryWords[secureRandom.nextInt(recoveryWords.size)]
        }
    }

    fun normalizePhrase(phrase: String): String =
        phrase.trim().lowercase().replace(Regex("\\s+"), " ")

    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)
}
