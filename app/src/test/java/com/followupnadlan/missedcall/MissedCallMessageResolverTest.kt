package com.followupnadlan.missedcall

import com.followupnadlan.templates.SprintOneTemplates
import com.followupnadlan.templates.TemplateStoreLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class MissedCallMessageResolverTest {
    @Test
    fun missedCallRenderPathUsesSelectedCustomBody() {
        val customBody = "saved custom body for missed-call send"
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(SprintOneTemplates.OPEN_ID to customBody)
        )

        val selected = MissedCallMessageResolver.selectedTemplate(
            templates = templates,
            selectedTemplateId = SprintOneTemplates.OPEN_ID
        )
        val rendered = MissedCallMessageResolver.renderTemplate(selected) { body -> "rendered: $body" }

        assertEquals("rendered: $customBody", rendered)
    }

    @Test
    fun orphanedSelectedIdFallsBackToFirstTemplateInsteadOfEmpty() {
        val selected = MissedCallMessageResolver.selectedTemplate(
            templates = SprintOneTemplates.all,
            selectedTemplateId = "deleted-card-id-that-no-longer-exists"
        )

        assertEquals(SprintOneTemplates.all.first().id, selected?.id)
    }
}
