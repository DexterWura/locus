package com.whereami.locus.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "locus_auth")

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

class AuthStore(private val context: Context) {
    private val passwordSaltKey = stringPreferencesKey("password_salt")
    private val passwordHashKey = stringPreferencesKey("password_hash")
    private val answerSaltKey = stringPreferencesKey("answer_salt")
    private val question1Key = stringPreferencesKey("question_1_id")
    private val question2Key = stringPreferencesKey("question_2_id")
    private val answer1HashKey = stringPreferencesKey("answer_1_hash")
    private val answer2HashKey = stringPreferencesKey("answer_2_hash")
    private val recoverySaltKey = stringPreferencesKey("recovery_salt")
    private val recoveryPhraseHashKey = stringPreferencesKey("recovery_phrase_hash")

    val isPasswordSet: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        !prefs[passwordHashKey].isNullOrBlank()
    }

    suspend fun getStoredAuth(): StoredAuth? {
        val prefs = context.authDataStore.data.first()
        val passwordSalt = prefs[passwordSaltKey] ?: return null
        val passwordHash = prefs[passwordHashKey] ?: return null
        val answerSalt = prefs[answerSaltKey] ?: return null
        val q1 = prefs[question1Key] ?: return null
        val q2 = prefs[question2Key] ?: return null
        val a1 = prefs[answer1HashKey] ?: return null
        val a2 = prefs[answer2HashKey] ?: return null
        val recoverySalt = prefs[recoverySaltKey] ?: return null
        val recoveryHash = prefs[recoveryPhraseHashKey] ?: return null
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
    ) {
        val passwordSalt = PasswordCrypto.generateSalt()
        val answerSalt = PasswordCrypto.generateSalt()
        val recoverySalt = PasswordCrypto.generateSalt()
        context.authDataStore.edit { prefs ->
            prefs[passwordSaltKey] = PasswordCrypto.encode(passwordSalt)
            prefs[passwordHashKey] = PasswordCrypto.encode(
                PasswordCrypto.hashSecret(password, passwordSalt)
            )
            prefs[answerSaltKey] = PasswordCrypto.encode(answerSalt)
            prefs[question1Key] = question1Id
            prefs[question2Key] = question2Id
            prefs[answer1HashKey] = PasswordCrypto.encode(
                PasswordCrypto.hashSecret(PasswordCrypto.normalizeAnswer(answer1), answerSalt)
            )
            prefs[answer2HashKey] = PasswordCrypto.encode(
                PasswordCrypto.hashSecret(PasswordCrypto.normalizeAnswer(answer2), answerSalt)
            )
            prefs[recoverySaltKey] = PasswordCrypto.encode(recoverySalt)
            prefs[recoveryPhraseHashKey] = PasswordCrypto.encode(
                PasswordCrypto.hashSecret(
                    PasswordCrypto.normalizePhrase(recoveryPhrase),
                    recoverySalt
                )
            )
        }
    }

    suspend fun verifyPassword(password: String): Boolean {
        val stored = getStoredAuth() ?: return false
        return PasswordCrypto.matches(password, stored.passwordSalt, stored.passwordHash)
    }

    suspend fun verifySecurityAnswers(answer1: String, answer2: String): Boolean {
        val stored = getStoredAuth() ?: return false
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
        return ok1 && ok2
    }

    suspend fun verifyRecoveryPhrase(phrase: String): Boolean {
        val stored = getStoredAuth() ?: return false
        return PasswordCrypto.matches(
            PasswordCrypto.normalizePhrase(phrase),
            stored.recoverySalt,
            stored.recoveryPhraseHash
        )
    }

    suspend fun updatePassword(newPassword: String) {
        val passwordSalt = PasswordCrypto.generateSalt()
        context.authDataStore.edit { prefs ->
            prefs[passwordSaltKey] = PasswordCrypto.encode(passwordSalt)
            prefs[passwordHashKey] = PasswordCrypto.encode(
                PasswordCrypto.hashSecret(newPassword, passwordSalt)
            )
        }
    }

    suspend fun questionIds(): Pair<String, String>? {
        val stored = getStoredAuth() ?: return null
        return stored.question1Id to stored.question2Id
    }
}
