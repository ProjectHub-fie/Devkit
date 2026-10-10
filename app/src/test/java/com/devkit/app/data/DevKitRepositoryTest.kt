package com.devkit.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.devkit.app.data.local.DevKitDatabase
import com.devkit.app.data.repo.DevKitRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Exercises the real Room database in memory (no mocks): persistence, redaction
 * on save, and the export/import round trip used by the API tester.
 */
@RunWith(RobolectricTestRunner::class)
class DevKitRepositoryTest {

    private lateinit var db: DevKitDatabase
    private lateinit var repository: DevKitRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DevKitDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = DevKitRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `favorites persist and toggle off`() = runTest {
        repository.toggleFavorite("json_formatter", true)
        assertEquals(1, repository.favorites.first().size)
        repository.toggleFavorite("json_formatter", false)
        assertTrue(repository.favorites.first().isEmpty())
    }

    @Test
    fun `tool history records usage`() = runTest {
        repository.recordToolUsage("base64")
        repository.recordToolUsage("uuid")
        assertEquals(2, repository.toolHistory.first().size)
    }

    @Test
    fun `history retention pruning removes old rows but keeps recent`() = runTest {
        val now = System.currentTimeMillis()
        val day = 24L * 60L * 60L * 1000L
        db.toolDao().recordUsage(
            com.devkit.app.data.local.ToolHistoryEntity(toolId = "old_tool", usedAt = now - 10 * day)
        )
        db.toolDao().recordUsage(
            com.devkit.app.data.local.ToolHistoryEntity(toolId = "new_tool", usedAt = now)
        )
        repository.pruneHistory(7)
        val remaining = repository.toolHistory.first().map { it.toolId }
        assertTrue("recent row should survive: $remaining", remaining.contains("new_tool"))
        assertFalse("old row should be pruned: $remaining", remaining.contains("old_tool"))
    }

    @Test
    fun `forever retention prunes nothing`() = runTest {
        val now = System.currentTimeMillis()
        db.toolDao().recordUsage(
            com.devkit.app.data.local.ToolHistoryEntity(toolId = "ancient", usedAt = now - 9999L * 24 * 60 * 60 * 1000)
        )
        repository.pruneHistory(0)
        assertEquals(1, repository.toolHistory.first().size)
    }

    @Test
    fun `clear all data empties every table`() = runTest {
        repository.recordToolUsage("uuid")
        repository.toggleFavorite("uuid", true)
        repository.saveSnippet("t", "kotlin", "x")
        repository.clearAllData()
        assertTrue(repository.toolHistory.first().isEmpty())
        assertTrue(repository.favorites.first().isEmpty())
        assertTrue(repository.snippets.first().isEmpty())
        assertTrue(repository.collections.first().isEmpty())
    }

    @Test
    fun `request history stores a redacted url`() = runTest {
        repository.recordRequest(
            method = "GET",
            displayUrl = "https://api.example.com/data?api_key=topsecret&x=1",
            statusCode = 200,
            durationMillis = 12,
            label = "OK",
        )
        val stored = repository.requestHistory().first().first()
        assertFalse(stored.displayUrl.contains("topsecret"))
    }

    @Test
    fun `snippet saves and deletes`() = runTest {
        val id = repository.saveSnippet("Hello", "kotlin", "println(1)")
        assertEquals(1, repository.snippets.first().size)
        repository.deleteSnippet(id)
        assertTrue(repository.snippets.first().isEmpty())
    }

    @Test
    fun `saved request stores a redacted display url`() = runTest {
        val id = repository.saveRequest(
            collectionId = null,
            name = "Get users",
            method = "GET",
            url = "https://api.dev/users?token=supersecret",
            headers = listOf("Authorization" to "Bearer supersecret"),
            queryParams = emptyList(),
            bodyType = "NONE",
            body = "",
            timeoutSeconds = 30,
        )
        val entity = repository.savedRequest(id)!!
        assertFalse(entity.displayUrl.contains("supersecret"))
        assertTrue(entity.displayUrl.contains("token="))
        // The raw URL is preserved for actually sending the request.
        assertTrue(entity.url.contains("supersecret"))
    }

    @Test
    fun `headers round trip through json serialization`() = runTest {
        val id = repository.saveRequest(
            collectionId = null,
            name = "Post",
            method = "POST",
            url = "https://api.dev/post",
            headers = listOf("Content-Type" to "application/json", "X-Trace" to "abc"),
            queryParams = listOf("page" to "2"),
            bodyType = "JSON",
            body = """{"a":1}""",
            timeoutSeconds = 15,
        )
        val entity = repository.savedRequest(id)!!
        val headers = repository.decodeHeaders(entity)
        assertEquals(2, headers.size)
        assertEquals("application/json", headers.first().value)
        assertEquals(1, repository.decodeParams(entity).size)
    }

    @Test
    fun `environment variables save and load`() = runTest {
        val id = repository.saveEnvironment("staging", mapOf("base_url" to "https://stg.dev", "token" to "t0ken"))
        val entity = repository.environments.first().first { it.id == id }
        val vars = repository.environmentVariables(entity)
        assertEquals("https://stg.dev", vars["base_url"])
        assertEquals("t0ken", vars["token"])
    }

    @Test
    fun `export redacts sensitive headers by default`() = runTest {
        repository.saveRequest(
            collectionId = null,
            name = "Secret",
            method = "GET",
            url = "https://api.dev/x",
            headers = listOf("Authorization" to "Bearer topsecret"),
            queryParams = emptyList(),
            bodyType = "NONE",
            body = "",
            timeoutSeconds = 30,
        )
        val exported = repository.exportCollections(includeSecrets = false)
        assertFalse("leaked: <$exported>", exported.contains("topsecret"))
        assertTrue("schema missing: <$exported>", exported.contains("devkit.collection.v1"))
    }

    @Test
    fun `export then import restores requests`() = runTest {
        val collectionId = repository.saveCollection("Demo")
        repository.saveRequest(
            collectionId = collectionId,
            name = "Ping",
            method = "GET",
            url = "https://api.dev/ping",
            headers = listOf("Accept" to "*/*"),
            queryParams = emptyList(),
            bodyType = "NONE",
            body = "",
            timeoutSeconds = 30,
        )
        val payload = repository.exportCollections(includeSecrets = true)

        // Fresh database, import the payload.
        val freshDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DevKitDatabase::class.java,
        ).allowMainThreadQueries().build()
        val freshRepo = DevKitRepository(freshDb)
        val result = freshRepo.importCollections(payload)
        assertTrue(result is DevKitRepository.ImportResult.Success)
        assertEquals(1, (result as DevKitRepository.ImportResult.Success).count)
        assertTrue(freshRepo.savedRequests.first().any { it.name == "Ping" })
        freshDb.close()
    }

    @Test
    fun `import rejects invalid json`() = runTest {
        assertTrue(repository.importCollections("not json") is DevKitRepository.ImportResult.Failure)
    }

    @Test
    fun `import rejects unsupported schema`() = runTest {
        val bad = """{"schema":"devkit.collection.v99","exportedAt":0,"collections":[]}"""
        val result = repository.importCollections(bad)
        assertTrue(result is DevKitRepository.ImportResult.Failure)
        assertTrue((result as DevKitRepository.ImportResult.Failure).message.contains("schema"))
    }

    @Test
    fun `import rejects blank payload`() = runTest {
        assertTrue(repository.importCollections("   ") is DevKitRepository.ImportResult.Failure)
    }

    @Test
    fun `clear all data wipes every table`() = runTest {
        repository.recordToolUsage("uuid")
        repository.toggleFavorite("json_formatter", true)
        repository.saveSnippet("s", "kt", "code")
        repository.saveCollection("c")
        repository.recordRequest("GET", "https://x.dev", 200, 10, "HTTP 200")
        repository.saveEnvironment("e", mapOf("k" to "v"))

        repository.clearAllData()

        assertTrue(repository.toolHistory.first().isEmpty())
        assertTrue(repository.favorites.first().isEmpty())
        assertTrue(repository.snippets.first().isEmpty())
        assertTrue(repository.collections.first().isEmpty())
        assertTrue(repository.requestHistory().first().isEmpty())
        assertTrue(repository.environments.first().isEmpty())
    }

    @Test
    fun `request history stores redacted urls`() = runTest {
        repository.recordRequest("GET", "https://api.dev/x?api_key=leaky", 200, 5, "HTTP 200")
        val entry = repository.requestHistory().first().first()
        assertFalse(entry.displayUrl.contains("leaky"))
    }
}
