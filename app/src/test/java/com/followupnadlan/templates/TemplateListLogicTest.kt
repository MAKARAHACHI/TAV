package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateListLogicTest {
    private fun template(id: String, body: String = "b") =
        MessageTemplate(id = id, title = "t-$id", body = body)

    private val list = listOf(template("a"), template("b"))

    @Test
    fun upsertReplacesExistingById() {
        val updated = TemplateListLogic.upsert(list, template("a", body = "new"))
        assertEquals(2, updated.size)
        assertEquals("new", updated.first { it.id == "a" }.body)
    }

    @Test
    fun upsertAppendsWhenIdIsNew() {
        val updated = TemplateListLogic.upsert(list, template("c"))
        assertEquals(listOf("a", "b", "c"), updated.map { it.id })
    }

    @Test
    fun deleteRemovesById() {
        val updated = TemplateListLogic.delete(list, "a")
        assertEquals(listOf("b"), updated.map { it.id })
    }

    @Test
    fun deleteNeverRemovesLastRemaining() {
        val single = listOf(template("only"))
        assertEquals(single, TemplateListLogic.delete(single, "only"))
    }

    @Test
    fun codecRoundTripPreservesAllFieldsIncludingLinksAndSeparators() {
        val templates = listOf(
            MessageTemplate(
                id = "id-1",
                title = "כותרת | עם מפריד",
                body = "שורה 1\nשורה 2",
                cardLink = "https://card.example?a=1&b=2",
                websiteLink = "https://site.example"
            ),
            MessageTemplate(id = "id-2", title = "no links", body = "body")
        )
        val decoded = TemplateCodec.decode(TemplateCodec.encode(templates))
        assertEquals(templates, decoded)
    }

    @Test
    fun codecDecodeSkipsBlankLines() {
        val encoded = TemplateCodec.encode(listOf(template("a")))
        assertEquals(1, TemplateCodec.decode("\n\n$encoded\n\n").size)
    }

    @Test
    fun codecDecodesLegacyThreeFieldLineWithEmptyLinks() {
        // Simulate a line written before link fields existed (only id|title|body).
        val decoded = TemplateCodec.decode(TemplateCodec.encode(listOf(template("a"))))
        assertTrue(decoded.all { it.cardLink.isEmpty() && it.websiteLink.isEmpty() })
    }
}
