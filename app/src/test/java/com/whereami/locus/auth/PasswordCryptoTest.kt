package com.whereami.locus.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordCryptoTest {

    @Test
    fun hash_and_match_roundTrip() {
        val salt = PasswordCrypto.generateSalt()
        val hash = PasswordCrypto.hashSecret("secret-pass", salt)
        assertTrue(PasswordCrypto.matches("secret-pass", salt, hash))
        assertFalse(PasswordCrypto.matches("wrong-pass", salt, hash))
    }

    @Test
    fun differentSalts_produceDifferentHashes() {
        val a = PasswordCrypto.generateSalt()
        val b = PasswordCrypto.generateSalt()
        assertNotEquals(
            PasswordCrypto.encode(PasswordCrypto.hashSecret("same", a)),
            PasswordCrypto.encode(PasswordCrypto.hashSecret("same", b))
        )
    }

    @Test
    fun normalizeAnswer_trimsAndLowercases() {
        assertEquals("blue lake", PasswordCrypto.normalizeAnswer("  Blue   Lake  "))
    }

    @Test
    fun recoveryPhrase_hasEightWords() {
        val phrase = PasswordCrypto.generateRecoveryPhrase()
        assertEquals(8, phrase.split(" ").size)
        assertTrue(PasswordCrypto.normalizePhrase("  $phrase  ") == phrase.lowercase())
    }
}
