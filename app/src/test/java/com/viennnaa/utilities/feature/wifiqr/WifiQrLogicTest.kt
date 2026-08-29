package com.viennnaa.utilities.feature.wifiqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiQrLogicTest {

    @Test
    fun `a plain WPA network`() {
        assertEquals(
            "WIFI:T:WPA;S:HomeNet;P:hunter2;;",
            wifiPayload("HomeNet", "hunter2", WifiSecurity.WPA),
        )
    }

    @Test
    fun `an open network omits the password field entirely`() {
        // Sending an empty P: field makes some phones prompt for a password.
        assertEquals(
            "WIFI:T:nopass;S:CafeGuest;;",
            wifiPayload("CafeGuest", "ignored", WifiSecurity.NONE),
        )
    }

    @Test
    fun `a hidden network carries the flag`() {
        assertEquals(
            "WIFI:T:WPA;S:Secret;P:pass;H:true;;",
            wifiPayload("Secret", "pass", WifiSecurity.WPA, hidden = true),
        )
    }

    @Test
    fun `WEP is carried through as its own type`() {
        assertTrue(wifiPayload("Old", "key", WifiSecurity.WEP)!!.startsWith("WIFI:T:WEP;"))
    }

    @Test
    fun `a semicolon in the password is escaped`() {
        // Unescaped, this would end the password field early and everything
        // after it would parse as another field.
        assertEquals(
            "WIFI:T:WPA;S:Net;P:pa\\;ss;;",
            wifiPayload("Net", "pa;ss", WifiSecurity.WPA),
        )
    }

    @Test
    fun `every special character is escaped`() {
        assertEquals("\\\\", escapeWifiValue("\\"))
        assertEquals("\\;", escapeWifiValue(";"))
        assertEquals("\\,", escapeWifiValue(","))
        assertEquals("\\:", escapeWifiValue(":"))
        assertEquals("\\\"", escapeWifiValue("\""))
    }

    @Test
    fun `a backslash is escaped once, not twice over`() {
        // Escaping the backslash after the others would double-escape the ones
        // those passes had just added.
        assertEquals("a\\\\b\\;c", escapeWifiValue("a\\b;c"))
    }

    @Test
    fun `ordinary characters are left alone`() {
        assertEquals("Plain Network 5G", escapeWifiValue("Plain Network 5G"))
        assertEquals("café", escapeWifiValue("café"))
        // An apostrophe has no meaning in the format, so it is not escaped.
        assertEquals("Joe's", escapeWifiValue("Joe's"))
    }

    @Test
    fun `an SSID with special characters is escaped too`() {
        assertEquals(
            "WIFI:T:WPA;S:Joe's\\:Net;P:pw;;",
            wifiPayload("Joe's:Net", "pw", WifiSecurity.WPA),
        )
    }

    @Test
    fun `an empty SSID has no payload`() {
        assertNull(wifiPayload("", "pass", WifiSecurity.WPA))
    }

    @Test
    fun `over-long values are trimmed to the standard limits`() {
        val payload = wifiPayload("s".repeat(100), "p".repeat(100), WifiSecurity.WPA)!!
        assertTrue(payload.contains("S:" + "s".repeat(MAX_SSID_LENGTH) + ";"))
        assertTrue(payload.contains("P:" + "p".repeat(MAX_PASSWORD_LENGTH) + ";"))
    }

    @Test
    fun `a secured network without a password is flagged`() {
        assertTrue(needsPassword(WifiSecurity.WPA, ""))
        assertTrue(needsPassword(WifiSecurity.WEP, ""))
        assertFalse(needsPassword(WifiSecurity.WPA, "pass"))
        // An open network never needs one.
        assertFalse(needsPassword(WifiSecurity.NONE, ""))
    }
}
