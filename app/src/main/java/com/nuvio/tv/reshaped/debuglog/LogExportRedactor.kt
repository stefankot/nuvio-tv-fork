package com.nuvio.tv.reshaped.debuglog

import java.net.URI

private val webUrl = Regex("""(?i)\bhttps?://[^\s"'<>\u0000-\u001f]+""")
private val privateHeader = Regex("""(?im)\b(authorization|proxy-authorization|cookie|set-cookie|x-api-key)\s*[:=][^\r\n]*""")
private val privateField = Regex("""(?i)\b(access_token|refresh_token|token|api[_-]?key|password|passwd)\s*=[^\s,;&]+""")

/** Keep host-level diagnostics without exporting playback paths, URL parameters or credentials. */
internal fun redactDiagnosticExport(body: String): String {
    val noUrls = webUrl.replace(body) { match ->
        val uri = runCatching { URI.create(match.value) }.getOrNull()
        val host = uri?.host
        if (uri == null || host == null) "[URL redacted]" else "${uri.scheme}://$host/[redacted]"
    }
    return privateField.replace(privateHeader.replace(noUrls) { "${it.groupValues[1]}: [redacted]" }) {
        "${it.groupValues[1]}=[redacted]"
    }
}
