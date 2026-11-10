package app.gagafinance.android

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** A portable, password-encrypted JSON envelope for the manual finance backup. */
object BackupPasswordPolicy {
    fun valid(password: String): Boolean =
        password.length in 12..128 && password.any { it.isLetter() } &&
            password.any { it.isDigit() }
}

/**
 * AES-256-GCM with 256-bit PBKDF2 derived key. Kept compatible with Android API 23.
 * The JSON envelope is not a database encryption scheme, and the password is NOT
 * stored in preferences or on GitHub. Lose the password -> lose this backup.
 */
object ProtectedBackup {
    private const val KIND = "gaga-finance-encrypted-backup"
    private const val VERSION = 1
    private const val ITERATIONS = 250_000
    private const val MAX_ENVELOPE = 12_000_000

    fun isProtected(raw: String): Boolean = try {
        JSONObject(raw).optString("format") == KIND
    } catch (_: Exception) { false }

    fun encrypt(plainJson: String, password: String): String {
        require(BackupPasswordPolicy.valid(password)) {
            "Use a password of 12–128 characters with letters and numbers"
        }
        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val nonce = ByteArray(12).also { random.nextBytes(it) }
        val key = derive(password, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        val encrypted = cipher.doFinal(plainJson.toByteArray(StandardCharsets.UTF_8))
        return JSONObject()
            .put("format", KIND)
            .put("version", VERSION)
            .put("kdf", "PBKDF2-HMAC-SHA1")
            .put("iterations", ITERATIONS)
            .put("cipher", "AES-256-GCM")
            .put("salt", encode(salt))
            .put("nonce", encode(nonce))
            .put("data", encode(encrypted))
            .toString(2)
    }

    fun decrypt(raw: String, password: String): String {
        require(raw.length <= MAX_ENVELOPE) { "Backup file exceeds safety limit" }
        require(password.isNotEmpty()) { "Enter the original backup password" }
        try {
            val obj = JSONObject(raw)
            require(obj.getString("format") == KIND &&
                obj.getInt("version") == VERSION &&
                obj.getString("kdf") == "PBKDF2-HMAC-SHA1" &&
                obj.getString("cipher") == "AES-256-GCM" &&
                obj.getInt("iterations") == ITERATIONS) {
                "Unsupported encrypted backup"
            }
            val salt = decode(obj.getString("salt"))
            val nonce = decode(obj.getString("nonce"))
            val data = decode(obj.getString("data"))
            require(salt.size == 16 && nonce.size == 12 &&
                data.size in 17..8_100_000) { "Invalid backup envelope" }
            val key = derive(password, salt, ITERATIONS)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
            return String(cipher.doFinal(data), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // Do not leak whether the password, content, or authentication tag failed.
            throw IllegalArgumentException("Incorrect password or damaged encrypted backup")
        }
    }

    private fun derive(password: String, salt: ByteArray, count: Int): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, count, 256)
        return try {
            val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(spec).encoded
            try { SecretKeySpec(raw, "AES") } finally { raw.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(text: String) = Base64.decode(text, Base64.NO_WRAP)
}
