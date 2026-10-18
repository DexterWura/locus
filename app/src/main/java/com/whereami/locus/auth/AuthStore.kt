package com.whereami.locus.auth

import android.content.Context
import android.content.SharedPreferences
import com.whereami.locus.data.security.EncryptedPrefs
import com.whereami.locus.data.security.LegacyDataStoreMigrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class StoredAuth(
    val passwordSalt: ByteArray,
    val passwordHash: ByteArray,
    val answerSalt: ByteArray,
    val question1Id: String,
    val question2Id: String,
    val answer1Hash: ByteArray,
    val answer2Hash: ByteArray,
    val recoverySalt: ByteArray,
    val recoveryPhraseHash: ByteArray
)

class AuthStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        EncryptedPrefs.create(appContext, PREFS_NAME).also { secure ->
            LegacyDataStoreMigrator.migrateAuth(appContext, secure)
        }

    private val _isPasswordSet = MutableStateFlow(prefs.contains(KEY_PASSWORD_HASH))
    val isPasswordSet: StateFlow<Boolean> = _isPasswordSet.asStateFlow()

    fun getStoredAuth(): StoredAuth? {
        val passwordSalt = prefs.getString(KEY_PASSWORD_SALT, null) ?: return null
        val passwordHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return null
        val answerSalt = prefs.getString(KEY_ANSWER_SALT, null) ?: return null
        val q1 = prefs.getString(KEY_QUESTION_1, null) ?: return null
        val q2 = prefs.getString(KEY_QUESTION_2, null) ?: return null
        val a1 = prefs.getString(KEY_ANSWER_1_HASH, null) ?: return null
        val a2 = prefs.getString(KEY_ANSWER_2_HASH, null) ?: return null
        val recoverySalt = prefs.getString(KEY_RECOVERY_SALT, null) ?: return null
        val recoveryHash = prefs.getString(KEY_RECOVERY_HASH, null) ?: return null
        return StoredAuth(
            passwordSalt = PasswordCrypto.decode(passwordSalt),
            passwordHash = PasswordCrypto.decode(passwordHash),
            answerSalt = PasswordCrypto.decode(answerSalt),
            question1Id = q1,
            question2Id = q2,
            answer1Hash = PasswordCrypto.decode(a1),
            answer2Hash = PasswordCrypto.decode(a2),
            recoverySalt = PasswordCrypto.decode(recoverySalt),
            recoveryPhraseHash = PasswordCrypto.decode(recoveryHash)
        )
    }

    suspend fun setup(
        password: String,
        question1Id: String,
        question2Id: String,
        answer1: String,
        answer2: String,
        recoveryPhrase: String
    ) = withContext(Dispatchers.Default) {
        val passwordSalt = PasswordCrypto.generateSalt()
        val answerSalt = PasswordCrypto.generateSalt()
        val recoverySalt = PasswordCrypto.generateSalt()
        prefs.edit()
            .putString(KEY_PASSWORD_SALT, PasswordCrypto.encode(passwordSalt))
            .putString(
                KEY_PASSWORD_HASH,
                PasswordCrypto.encode(PasswordCrypto.hashSecret(password, passwordSalt))
            )
            .putString(KEY_ANSWER_SALT, PasswordCrypto.encode(answerSalt))
            .putString(KEY_QUESTION_1, question1Id)
            .putString(KEY_QUESTION_2, question2Id)
            .putString(
                KEY_ANSWER_1_HASH,
                PasswordCrypto.encode(
                    PasswordCrypto.hashSecret(PasswordCrypto.normalizeAnswer(answer1), answerSalt)
                )
            )
            .putString(
                KEY_ANSWER_2_HASH,
                PasswordCrypto.encode(
                    PasswordCrypto.hashSecret(PasswordCrypto.normalizeAnswer(answer2), answerSalt)
                )
            )
            .putString(KEY_RECOVERY_SALT, PasswordCrypto.encode(recoverySalt))
            .putString(
                KEY_RECOVERY_HASH,
                PasswordCrypto.encode(
                    PasswordCrypto.hashSecret(
                        PasswordCrypto.normalizePhrase(recoveryPhrase),
                        recoverySalt
                    )
                )
            )
            .apply()
        _isPasswordSet.value = true
    }

    suspend fun verifyPassword(password: String): Boolean = withContext(Dispatchers.Default) {
        val stored = getStoredAuth() ?: return@withContext false
        PasswordCrypto.matches(password, stored.passwordSalt, stored.passwordHash)
    }

    suspend fun verifySecurityAnswers(answer1: String, answer2: String): Boolean =
        withContext(Dispatchers.Default) {
            val stored = getStoredAuth() ?: return@withContext false
            val ok1 = PasswordCrypto.matches(
                PasswordCrypto.normalizeAnswer(answer1),
                stored.answerSalt,
                stored.answer1Hash
            )
            val ok2 = PasswordCrypto.matches(
                PasswordCrypto.normalizeAnswer(answer2),
                stored.answerSalt,
                stored.answer2Hash
            )
            ok1 && ok2
        }

    suspend fun verifyRecoveryPhrase(phrase: String): Boolean = withContext(Dispatchers.Default) {
        val stored = getStoredAuth() ?: return@withContext false
        PasswordCrypto.matches(
            PasswordCrypto.normalizePhrase(phrase),
            stored.recoverySalt,
            stored.recoveryPhraseHash
        )
    }

    suspend fun updatePassword(newPassword: String) = withContext(Dispatchers.Default) {
        val passwordSalt = PasswordCrypto.generateSalt()
        prefs.edit()
            .putString(KEY_PASSWORD_SALT, PasswordCrypto.encode(passwordSalt))
            .putString(
                KEY_PASSWORD_HASH,
                PasswordCrypto.encode(PasswordCrypto.hashSecret(newPassword, passwordSalt))
            )
            .apply()
    }

    fun questionIds(): Pair<String, String>? {
        val stored = getStoredAuth() ?: return null
        return stored.question1Id to stored.question2Id
    }

    companion object {
        private const val PREFS_NAME = "locus_auth_secure"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_ANSWER_SALT = "answer_salt"
        private const val KEY_QUESTION_1 = "question_1_id"
        private const val KEY_QUESTION_2 = "question_2_id"
        private const val KEY_ANSWER_1_HASH = "answer_1_hash"
        private const val KEY_ANSWER_2_HASH = "answer_2_hash"
        private const val KEY_RECOVERY_SALT = "recovery_salt"
        private const val KEY_RECOVERY_HASH = "recovery_phrase_hash"
    }
}
