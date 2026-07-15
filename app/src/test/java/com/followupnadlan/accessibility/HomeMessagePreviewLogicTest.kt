package com.followupnadlan.accessibility

import com.followupnadlan.templates.SprintOneTemplates
import com.followupnadlan.templates.TemplateStoreLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeMessagePreviewLogicTest {
    @Test
    fun homeMessagePreviewReadsSelectedCustomBody() {
        val customBody = """
            first custom line
            second custom line
            third custom line
            fourth custom line
        """.trimIndent()
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(SprintOneTemplates.PRIVATE_ID to customBody)
        )

        val preview = HomeMessagePreviewLogic.preview(
            templates = templates,
            selectedTemplateId = SprintOneTemplates.PRIVATE_ID
        )

        assertEquals("first custom line\nsecond custom line\nthird custom line", preview)
    }

    @Test
    fun homePreviewUpdatesAfterInlineEditSave() {
        val editedBody = """
            edited saved message
            second saved line
        """.trimIndent()
        val templatesAfterSave = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(SprintOneTemplates.GENTLE_ID to editedBody)
        )

        val preview = HomeMessagePreviewLogic.preview(
            templates = templatesAfterSave,
            selectedTemplateId = SprintOneTemplates.GENTLE_ID
        )

        assertEquals("edited saved message\nsecond saved line", preview)
    }

    @Test
    fun whatsappStylePreviewReadsCurrentSavedMessage() {
        val savedMessage = "this is what the caller will receive"
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(SprintOneTemplates.OPEN_ID to savedMessage)
        )

        val preview = HomeMessagePreviewLogic.preview(
            templates = templates,
            selectedTemplateId = SprintOneTemplates.OPEN_ID
        )

        assertEquals(savedMessage, preview)
    }
}
