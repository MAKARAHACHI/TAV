package com.followupnadlan.accessibility

import com.followupnadlan.templates.MessageComposition
import com.followupnadlan.templates.MessageTemplate
import com.followupnadlan.templates.SprintOneTemplates
import com.followupnadlan.templates.TemplateStoreLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeMessagePreviewLogicTest {
    @Test
    fun homeMessagePreviewShowsFullBodyNotTruncated() {
        val customBody = """
            first custom line
            second custom line
            third custom line
            fourth custom line
        """.trimIndent()
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(SprintOneTemplates.MISSED_ID to customBody)
        )

        val preview = HomeMessagePreviewLogic.preview(
            templates = templates,
            selectedTemplateId = SprintOneTemplates.MISSED_ID
        )

        // Home now shows exactly what will be sent — the full message, not a 3-line teaser.
        assertEquals(customBody, preview)
    }

    @Test
    fun homeMessagePreviewAppendsLinksSoItMatchesWhatIsSent() {
        val card = MessageTemplate(
            id = "card-with-links",
            title = "English",
            body = "Hi, please text me here.",
            cardLink = "https://card.example/me",
            websiteLink = "https://example.com"
        )

        val preview = HomeMessagePreviewLogic.preview(
            templates = listOf(card),
            selectedTemplateId = card.id
        )

        // Must equal the full composed message the engine sends (body + link lines).
        assertEquals(MessageComposition.build(card), preview)
        assertEquals(true, preview.contains(MessageComposition.CARD_LABEL))
        assertEquals(true, preview.contains(MessageComposition.WEBSITE_LABEL))
    }

    @Test
    fun homeMessagePreviewFallsBackToFirstCardWhenIdOrphaned() {
        val preview = HomeMessagePreviewLogic.preview(
            templates = SprintOneTemplates.all,
            selectedTemplateId = "deleted-id"
        )

        assertEquals(MessageComposition.build(SprintOneTemplates.all.first()), preview)
    }

    @Test
    fun homePreviewUpdatesAfterInlineEditSave() {
        val editedBody = """
            edited saved message
            second saved line
        """.trimIndent()
        val templatesAfterSave = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(SprintOneTemplates.ENDED_ID to editedBody)
        )

        val preview = HomeMessagePreviewLogic.preview(
            templates = templatesAfterSave,
            selectedTemplateId = SprintOneTemplates.ENDED_ID
        )

        assertEquals("edited saved message\nsecond saved line", preview)
    }

    @Test
    fun whatsappStylePreviewReadsCurrentSavedMessage() {
        val savedMessage = "this is what the caller will receive"
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(SprintOneTemplates.ENDED_ID to savedMessage)
        )

        val preview = HomeMessagePreviewLogic.preview(
            templates = templates,
            selectedTemplateId = SprintOneTemplates.ENDED_ID
        )

        assertEquals(savedMessage, preview)
    }
}
