package com.example.data.local

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Local demonstration password handling only. Real production login MUST use server-side identity. */
object LocalPasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 150_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val PREFIX = "pbkdf2-sha256"

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val result = derive(password, salt, ITERATIONS)
        return "$PREFIX:$ITERATIONS:${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(result, Base64.NO_WRAP)}"
    }

    fun verify(password: String, stored: String): Boolean {
        if (!stored.startsWith("$PREFIX:")) {
            // Read compatibility for the original on-device demonstration database.
            // Callers upgrade any matching legacy plaintext entry immediately.
            return MessageDigest.isEqual(password.toByteArray(), stored.toByteArray())
        }
        val parts = stored.split(':')
        if (parts.size != 4) return false
        return try {
            val iterations = parts[1].toInt()
            if (iterations !in 100_000..500_000) return false
            val salt = Base64.decode(parts[2], Base64.DEFAULT)
            val expected = Base64.decode(parts[3], Base64.DEFAULT)
            if (salt.size != SALT_BYTES || expected.size != KEY_BITS / 8) return false
            MessageDigest.isEqual(expected, derive(password, salt, iterations))
        } catch (_: Exception) {
            false
        }
    }

    fun isHashed(stored: String): Boolean = stored.startsWith("$PREFIX:")

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
