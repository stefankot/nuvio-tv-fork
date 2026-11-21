package com.nuvio.tv.reshaped.debuglog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogExportRedactorTest {
    @Test
    fun exportedLogsKeepHostsAndTimingsButRemoveCredentials() {
        val log = """
            182ms GET https://example-user:example-pass@provider.example/user/pass/movie.mkv?key=123456
            authorization: Bearer example-secret
            retry token=next-secret
        """.trimIndent()
        val exported = redactDiagnosticExport(log)
        assertTrue(exported.contains("182ms GET https://provider.example/"))
        listOf("example-user", "example-pass", "movie.mkv", "123456", "example-secret", "next-secret").forEach {
            assertFalse(exported.contains(it))
        }
    }
}
