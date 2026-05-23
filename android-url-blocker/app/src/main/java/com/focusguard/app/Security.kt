package com.focusguard.app

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Master-password hashing. PBKDF2-HMAC-SHA256 with a per-install random salt
 * and a high iteration count, so a stored hash is expensive to brute-force.
 */
object Security {

    private const val ITERATIONS = 200_000
    private const val KEY_LENGTH = 256

    fun hash(password: CharArray, salt: ByteArray): String {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun encodeSalt(salt: ByteArray): String = Base64.encodeToString(salt, Base64.NO_WRAP)

    fun decodeSalt(encoded: String): ByteArray = Base64.decode(encoded, Base64.NO_WRAP)

    /** Constant-time comparison to avoid timing leaks. */
    fun slowEquals(a: String, b: String): Boolean {
        val ab = a.toByteArray()
        val bb = b.toByteArray()
        var diff = ab.size xor bb.size
        var i = 0
        while (i < ab.size && i < bb.size) {
            diff = diff or (ab[i].toInt() xor bb[i].toInt())
            i++
        }
        return diff == 0
    }
}
