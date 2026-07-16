package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateStoreTest {
    private val builtInTemplates = listOf(
        MessageTemplate(
            id = "buyer_property_details",
            title = "Buyer details",
            body = "Built in buyer"
        ),
        MessageTemplate(
            id = "missed_call",
            title = "Missed call",
            body = "Built in missed"
        )
    )

    @Test
    fun appliesSavedTemplateBodyOverBuiltInDefault() {
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = builtInTemplates,
            savedBodiesById = mapOf("buyer_property_details" to "Saved buyer {property_link}")
        )

        assertEquals("Saved buyer {property_link}", templates[0].body)
        assertEquals("Built in missed", templates[1].body)
    }

    @Test
    fun keepsBuiltInBodyWhenSavedValueIsMissing() {
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = builtInTemplates,
            savedBodiesById = emptyMap()
        )

        assertEquals("Built in buyer", templates[0].body)
        assertEquals("Built in missed", templates[1].body)
    }

    @Test
    fun preservesEmptySavedTemplateBody() {
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = builtInTemplates,
            savedBodiesById = mapOf("missed_call" to "")
        )

        assertEquals("Built in buyer", templates[0].body)
        assertEquals("", templates[1].body)
    }

    @Test
    fun editingEndedTemplateSavesAndReloadsCustomBody() {
        assertSavedAccessibilityBodySurvivesReload(SprintOneTemplates.ENDED_ID, "custom ended body")
    }

    @Test
    fun editingMissedTemplateSavesAndReloadsCustomBody() {
        assertSavedAccessibilityBodySurvivesReload(SprintOneTemplates.MISSED_ID, "custom missed body")
    }

    @Test
    fun selectedTemplateCustomBodyOverridesBuiltInDefault() {
        val customBody = "selected custom body"
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(SprintOneTemplates.ENDED_ID to customBody)
        )

        val selected = TemplateStoreLogic.selectedTemplate(templates, SprintOneTemplates.ENDED_ID)

        assertEquals(customBody, selected?.body)
    }

    @Test
    fun defaultEndedTemplateIsCompletedCallCopy() {
        val template = SprintOneTemplates.all.first { it.id == SprintOneTemplates.ENDED_ID }
        assertEquals("סיום שיחה", template.title)
        assertEquals(TemplateRole.CALL_ENDED, template.role)
        assert(template.body.startsWith("שלום, שמחתי לשוחח איתך.")) { "unexpected ended body: ${template.body}" }
    }

    @Test
    fun defaultMissedTemplateIsMissedCallCopy() {
        val template = SprintOneTemplates.all.first { it.id == SprintOneTemplates.MISSED_ID }
        assertEquals("שיחה שלא נענתה", template.title)
        assertEquals(TemplateRole.MISSED_CALL, template.role)
        // Must not claim to have "seen" the call (no live-read implication).
        assert(!template.body.contains("ראיתי")) { "missed body must not use 'ראיתי': ${template.body}" }
    }

    @Test
    fun freshInstallDefaultsAreEndedAndMissed() {
        assertEquals(SprintOneTemplates.ENDED_ID, SprintOneTemplates.DEFAULT_ID)
        assertEquals(SprintOneTemplates.MISSED_ID, SprintOneTemplates.DEFAULT_MISSED_ID)
        assertEquals(2, SprintOneTemplates.all.size)
    }

    @Test
    fun noDefaultTemplateContainsBusinessOrNadlanLanguage() {
        val banned = listOf(
            "נדל", "נדל״ן", "דירה", "נכס", "לקוח", "סוכן", "תיווך", "עסק",
            "כרטיס ביקור", "FollowUp", "Lead", "Pipeline", "CRM", "{{businessName}}"
        )
        SprintOneTemplates.all.forEach { template ->
            banned.forEach { word ->
                assert(!template.title.contains(word)) { "title '${template.title}' contains '$word'" }
                assert(!template.body.contains(word)) { "body of ${template.id} contains '$word'" }
            }
        }
    }

    private fun assertSavedAccessibilityBodySurvivesReload(templateId: String, customBody: String) {
        val firstLoad = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(templateId to customBody)
        )
        assertEquals(customBody, firstLoad.first { it.id == templateId }.body)

        val reload = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(templateId to firstLoad.first { it.id == templateId }.body)
        )
        assertEquals(customBody, reload.first { it.id == templateId }.body)
    }
}
