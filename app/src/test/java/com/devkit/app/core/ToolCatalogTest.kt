package com.devkit.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the promise that every tool the user can see is implemented: ids must
 * be unique, routes unique, and each tool discoverable via search.
 */
class ToolCatalogTest {

    @Test
    fun `tool ids are unique`() {
        val ids = ToolCatalog.tools.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `tool routes are unique`() {
        val routes = ToolCatalog.tools.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `every tool has a non-empty description and category`() {
        ToolCatalog.tools.forEach {
            assertTrue("${it.id} description", it.description.isNotBlank())
            assertTrue("${it.id} title", it.title.isNotBlank())
        }
    }

    @Test
    fun `byId resolves a known tool and rejects unknown`() {
        assertEquals("JSON Formatter", ToolCatalog.byId("json_formatter")?.title)
        assertEquals(null, ToolCatalog.byId("does_not_exist"))
    }

    @Test
    fun `search finds tools by keyword and description`() {
        assertTrue(ToolCatalog.search("base64").any { it.id == "base64" })
        assertTrue(ToolCatalog.search("timestamp").any { it.id == "timestamp" })
        assertTrue(ToolCatalog.search("json").isNotEmpty())
    }

    @Test
    fun `search is case insensitive and trims`() {
        assertEquals(
            ToolCatalog.search("uuid").map { it.id },
            ToolCatalog.search("  UUID  ").map { it.id },
        )
    }

    @Test
    fun `search for gibberish returns nothing`() {
        assertTrue(ToolCatalog.search("zzzzzznotatool").isEmpty())
    }

    @Test
    fun `every category has at least one tool`() {
        ToolCategory.entries.forEach { category ->
            assertTrue("category ${category.id}", ToolCatalog.byCategory(category).isNotEmpty())
        }
    }
}
