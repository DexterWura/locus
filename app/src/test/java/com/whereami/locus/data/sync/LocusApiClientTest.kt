package com.whereami.locus.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocusApiClientTest {
    @Test
    fun parsePairingQr_readsValidPayload() {
        val raw = """{"app":"locus","v":1,"server":"https://erp.example.com/","code":"abc123"}"""
        val payload = LocusApiClient.parsePairingQr(raw)
        assertNotNull(payload)
        assertEquals("https://erp.example.com", payload!!.serverUrl)
        assertEquals("abc123", payload.code)
    }

    @Test
    fun parsePairingQr_rejectsUnknownApp() {
        val raw = """{"app":"other","v":1,"server":"https://erp.example.com","code":"abc123"}"""
        assertNull(LocusApiClient.parsePairingQr(raw))
    }
}
