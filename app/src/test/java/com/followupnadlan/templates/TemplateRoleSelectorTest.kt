package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateRoleSelectorTest {
    private val ended1 = MessageTemplate("e1", "Ended 1", "ended body 1", role = TemplateRole.CALL_ENDED)
    private val ended2 = MessageTemplate("e2", "Ended 2", "ended body 2", role = TemplateRole.CALL_ENDED)
    private val missed1 = MessageTemplate("m1", "Missed 1", "missed body 1", role = TemplateRole.MISSED_CALL)
    private val all = listOf(ended1, missed1, ended2)

    @Test
    fun returnsChosenDefaultWhenItExistsAndMatchesRole() {
        val result = TemplateRoleSelector.forRole(all, TemplateRole.CALL_ENDED, "e2")
        assertEquals("e2", result?.id)
    }

    @Test
    fun ignoresChosenIdFromAnotherRoleAndFallsBackToRole() {
        // "m1" is a missed card; asking for a CALL_ENDED default must not return it.
        val result = TemplateRoleSelector.forRole(all, TemplateRole.CALL_ENDED, "m1")
        assertEquals("e1", result?.id)
    }

    @Test
    fun fallsBackToFirstCardOfRoleWhenChosenIdMissing() {
        val result = TemplateRoleSelector.forRole(all, TemplateRole.MISSED_CALL, "does-not-exist")
        assertEquals("m1", result?.id)
    }

    @Test
    fun fallsBackToAnyCardWhenNoCardOfRoleExists() {
        // No missed cards at all → send an ended card rather than nothing.
        val endedOnly = listOf(ended1, ended2)
        val result = TemplateRoleSelector.forRole(endedOnly, TemplateRole.MISSED_CALL, "m1")
        assertEquals("e1", result?.id)
    }

    @Test
    fun returnsNullOnlyWhenNoTemplatesAtAll() {
        assertEquals(null, TemplateRoleSelector.forRole(emptyList(), TemplateRole.MISSED_CALL, "m1"))
    }
}
