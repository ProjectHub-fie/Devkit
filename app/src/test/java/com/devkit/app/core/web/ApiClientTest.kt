package com.devkit.app.core.web

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    /** Production client still blocks cleartext except for the loopback test host. */
    private fun client() = ApiClient(cleartextTestHosts = setOf("localhost", "127.0.0.1"))

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `successful GET returns status body and headers`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"ok":true}""")
        )
        val outcome = client().execute(ApiRequest(url = server.url("/data").toString()))
        val response = (outcome as ApiOutcome.Success).response
        assertEquals(200, response.statusCode)
        assertTrue(response.body.contains("ok"))
        assertTrue(response.headers.any { it.first.equals("Content-Type", true) })
        assertTrue(response.durationMillis >= 0)
    }

    @Test
    fun `http error codes are surfaced normally`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("missing"))
        val outcome = client().execute(ApiRequest(url = server.url("/missing").toString()))
        assertEquals(404, (outcome as ApiOutcome.Success).response.statusCode)
    }

    @Test
    fun `post body and content type are sent`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val request = ApiRequest(
            method = HttpMethod.POST,
            url = server.url("/post").toString(),
            bodyType = BodyType.JSON,
            body = """{"a":1}""",
        )
        client().execute(request)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("""{"a":1}""", recorded.body.readUtf8())
        assertTrue(recorded.getHeader("Content-Type")!!.contains("application/json"))
    }

    @Test
    fun `query params are appended to the url`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val request = ApiRequest(
            url = server.url("/search").toString(),
            queryParams = listOf(QueryParam("q", "dev kit"), QueryParam("page", "2")),
        )
        client().execute(request)
        val recorded = server.takeRequest()
        assertTrue(recorded.path!!.contains("q=dev%20kit"))
        assertTrue(recorded.path!!.contains("page=2"))
    }

    @Test
    fun `cleartext http is blocked`() = runTest {
        val outcome = ApiClient().execute(ApiRequest(url = "http://example.com/"))
        assertEquals(ApiOutcome.NetworkError.Kind.CLEARTEXT_BLOCKED, (outcome as ApiOutcome.NetworkError).kind)
    }

    @Test
    fun `invalid url is reported`() = runTest {
        val outcome = client().execute(ApiRequest(url = "https://"))
        assertTrue(outcome is ApiOutcome.NetworkError)
    }

    @Test
    fun `empty url is rejected`() = runTest {
        val outcome = client().execute(ApiRequest(url = "   "))
        assertEquals(ApiOutcome.NetworkError.Kind.INVALID_URL, (outcome as ApiOutcome.NetworkError).kind)
    }

    @Test
    fun `oversized response is refused to protect memory`() = runTest {
        val big = "x".repeat((ApiClient.MAX_RESPONSE_BYTES + 1024).toInt())
        server.enqueue(MockResponse().setResponseCode(200).setBody(big))
        val outcome = client().execute(ApiRequest(url = server.url("/big").toString()))
        assertEquals(ApiOutcome.NetworkError.Kind.TOO_LARGE, (outcome as ApiOutcome.NetworkError).kind)
    }

    @Test
    fun `dns failure is reported as a network error`() = runTest {
        val outcome = client().execute(
            ApiRequest(url = "https://this-host-should-not-exist-devkit.invalid/")
        )
        assertTrue(outcome is ApiOutcome.NetworkError)
    }
}
