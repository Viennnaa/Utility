package com.viennnaa.utilities.feature.wifiqr

/**
 * Builds the payload a phone camera reads to join a network.
 *
 * The format is `WIFI:T:<auth>;S:<ssid>;P:<password>;H:<hidden>;;`. Its fields
 * are separated by semicolons and colons, so any of those inside a network name
 * or password has to be escaped or the payload parses into the wrong fields —
 * which shows up as a network that silently will not join.
 */

/** How the network is secured. */
enum class WifiSecurity(val token: String) {
    WPA("WPA"),
    WEP("WEP"),

    /** An open network. The password field is omitted entirely. */
    NONE("nopass"),
}

/** Backslash, semicolon, comma, colon and double quote all need escaping. */
private const val ESCAPED_CHARACTERS = "\\;,:\""

/** Longest values accepted, matching what the standards allow. */
const val MAX_SSID_LENGTH = 32
const val MAX_PASSWORD_LENGTH = 63

/**
 * Escapes the characters the format gives meaning to. The backslash is escaped
 * first, or escaping the others would double-escape the backslashes it added.
 */
fun escapeWifiValue(value: String): String {
    val out = StringBuilder(value.length)
    for (char in value) {
        if (char in ESCAPED_CHARACTERS) out.append('\\')
        out.append(char)
    }
    return out.toString()
}

/**
 * Builds the payload, or null when there is no network name to join — an empty
 * SSID would produce a code that scans but does nothing.
 */
fun wifiPayload(
    ssid: String,
    password: String,
    security: WifiSecurity,
    hidden: Boolean = false,
): String? {
    val trimmedSsid = ssid.take(MAX_SSID_LENGTH)
    if (trimmedSsid.isEmpty()) return null

    val builder = StringBuilder("WIFI:")
    builder.append("T:").append(security.token).append(';')
    builder.append("S:").append(escapeWifiValue(trimmedSsid)).append(';')
    if (security != WifiSecurity.NONE) {
        builder.append("P:").append(escapeWifiValue(password.take(MAX_PASSWORD_LENGTH))).append(';')
    }
    if (hidden) builder.append("H:true;")
    builder.append(';')
    return builder.toString()
}

/** True when the network needs a password before the code is worth showing. */
fun needsPassword(security: WifiSecurity, password: String): Boolean =
    security != WifiSecurity.NONE && password.isEmpty()
