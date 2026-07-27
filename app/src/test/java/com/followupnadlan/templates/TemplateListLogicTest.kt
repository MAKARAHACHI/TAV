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

    private fun roleTemplate(id: String, role: TemplateRole) =
        MessageTemplate(id = id, title = "t-$id", body = "b", role = role)

    @Test
    fun canAddForRoleStopsAtFive() {
        val ended = (1..5).map { roleTemplate("e$it", TemplateRole.CALL_ENDED) }
        // Five ended variants → cannot add a sixth of that role.
        assertTrue(!TemplateListLogic.canAddForRole(ended, TemplateRole.CALL_ENDED))
        // But another role is unaffected by the ended count.
        assertTrue(TemplateListLogic.canAddForRole(ended, TemplateRole.MISSED_CALL))
        // Four is still under the cap.
        assertTrue(TemplateListLogic.canAddForRole(ended.take(4), TemplateRole.CALL_ENDED))
    }

    @Test
    fun countForRoleCountsOnlyThatRole() {
        val mixed = listOf(
            roleTemplate("e1", TemplateRole.CALL_ENDED),
            roleTemplate("e2", TemplateRole.CALL_ENDED),
            roleTemplate("m1", TemplateRole.MISSED_CALL),
            roleTemplate("n1", TemplateRole.NO_ANSWER_OUTGOING)
        )
        assertEquals(2, TemplateListLogic.countForRole(mixed, TemplateRole.CALL_ENDED))
        assertEquals(1, TemplateListLogic.countForRole(mixed, TemplateRole.NO_ANSWER_OUTGOING))
    }

    @Test
    fun deleteWithinRoleKeepsLastOfThatRoleButDeletesWhenOthersExist() {
        val list = listOf(
            roleTemplate("e1", TemplateRole.CALL_ENDED),
            roleTemplate("e2", TemplateRole.CALL_ENDED),
            roleTemplate("m1", TemplateRole.MISSED_CALL)
        )
        // Two ended → one can go.
        assertEquals(listOf("e1", "m1"), TemplateListLogic.deleteWithinRole(list, "e2").map { it.id })
        // The single missed variant is protected even though the whole list has 3 entries.
        assertEquals(list, TemplateListLogic.deleteWithinRole(list, "m1"))
    }

    @Test
    fun ensureRoleDefaultsSeedsOnlyMissingRoles() {
        val existing = listOf(
            roleTemplate("e1", TemplateRole.CALL_ENDED),
            roleTemplate("m1", TemplateRole.MISSED_CALL)
        )
        val defaults = listOf(
            roleTemplate("de", TemplateRole.CALL_ENDED),
            roleTemplate("dn", TemplateRole.NO_ANSWER_OUTGOING)
        )
        val result = TemplateListLogic.ensureRoleDefaults(existing, defaults)
        // The NO_ANSWER default is appended; the already-present CALL_ENDED default is not.
        assertEquals(listOf("e1", "m1", "dn"), result.map { it.id })
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
