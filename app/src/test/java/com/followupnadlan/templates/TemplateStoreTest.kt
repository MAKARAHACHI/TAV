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
    fun editingOpenTemplateSavesAndReloadsCustomBody() {
        assertSavedAccessibilityBodySurvivesReload(SprintOneTemplates.OPEN_ID, "custom open body")
    }

    @Test
    fun editingGentleTemplateSavesAndReloadsCustomBody() {
        assertSavedAccessibilityBodySurvivesReload(SprintOneTemplates.GENTLE_ID, "custom gentle body")
    }

    @Test
    fun editingPrivateTemplateSavesAndReloadsCustomBody() {
        assertSavedAccessibilityBodySurvivesReload(SprintOneTemplates.PRIVATE_ID, "custom private body")
    }

    @Test
    fun selectedTemplateCustomBodyOverridesBuiltInDefault() {
        val customBody = "selected custom body"
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(SprintOneTemplates.GENTLE_ID to customBody)
        )

        val selected = TemplateStoreLogic.selectedTemplate(templates, SprintOneTemplates.GENTLE_ID)

        assertEquals(customBody, selected?.body)
    }

    @Test
    fun defaultOpenTemplateReturnsAccessibilityCopy() {
        val template = SprintOneTemplates.all.first { it.id == SprintOneTemplates.OPEN_ID }
        assertEquals("גלוי", template.title)
        assertEquals(
            "שלום, אני חירש/ת או כבד/ת שמיעה ולא תמיד יכול/ה לענות לשיחה קולית.\n" +
                "אפשר לכתוב לי כאן ב־WhatsApp או ב־SMS ואחזור אליך בכתב.",
            template.body
        )
    }

    @Test
    fun defaultGentleTemplateReturnsAccessibilityCopy() {
        val template = SprintOneTemplates.all.first { it.id == SprintOneTemplates.GENTLE_ID }
        assertEquals("עדין", template.title)
        assertEquals(
            "שלום, קשה לי לענות לשיחות קוליות.\n" +
                "אפשר בבקשה לכתוב לי כאן ב־WhatsApp או ב־SMS?",
            template.body
        )
    }

    @Test
    fun defaultPrivateTemplateReturnsAccessibilityCopy() {
        val template = SprintOneTemplates.all.first { it.id == SprintOneTemplates.PRIVATE_ID }
        assertEquals("פרטי", template.title)
        assertEquals(
            "שלום, אני מעדיף/ה תקשורת בכתב.\n" +
                "אפשר לכתוב לי כאן ואחזור אליך בהודעה.",
            template.body
        )
    }

    @Test
    fun freshInstallDefaultTemplateIsGentle() {
        assertEquals(SprintOneTemplates.GENTLE_ID, SprintOneTemplates.DEFAULT_ID)
        assertEquals("עדין", SprintOneTemplates.all.first { it.id == SprintOneTemplates.DEFAULT_ID }.title)
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
