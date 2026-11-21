package com.nuvio.tv.reshaped.livetv

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LiveTvHttpRevalidationTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun revalidationKeepsTheLastValidFileAndScopesHeadersToIt() = runBlocking {
        MockWebServer().use { server ->
            val file = folder.newFile("guide.xml.gz").apply { delete() }
            val url = server.url("/guide").toString()
            suspend fun fetch(headers: Map<String, String> = emptyMap(), revalidate: Boolean = true): String? =
                LiveTvHttp.downloadReading(
                    url, headers, file, 10, read = { it.bufferedReader().readText() },
                    keep = { it.startsWith("<tv") }, revalidate = revalidate,
                )

            server.enqueue(MockResponse().setHeader("ETag", "\"abc\"").setBody("<tv />"))
            assertEquals("<tv />", fetch())
            assertNull(server.takeRequest().getHeader("If-None-Match"))
            val body = file.readBytes().toList()

            server.enqueue(MockResponse().setResponseCode(304))
            assertNull(fetch())
            assertEquals("\"abc\"", server.takeRequest().getHeader("If-None-Match"))
            assertEquals(body, file.readBytes().toList())

            server.enqueue(MockResponse().setHeader("ETag", "\"other\"").setBody("error body"))
            assertEquals("error body", fetch(mapOf("X-Profile" to "alternate")))
            assertNull(server.takeRequest().getHeader("If-None-Match"))
            assertEquals(body, file.readBytes().toList())

            server.enqueue(MockResponse().setBody("<tv />"))
            assertEquals("<tv />", fetch(revalidate = false))
            assertNull(server.takeRequest().getHeader("If-None-Match"))
        }
    }
}
