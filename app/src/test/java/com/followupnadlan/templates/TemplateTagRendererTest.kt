package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateTagRendererTest {
    @Test
    fun rendersAllSupportedTags() {
        val rendered = TemplateTagRenderer.render(
            "{lead_name}|{agent_name}|{office_name}|{phone}|{website}|{business_card}|{signature}",
            TemplateTagValues(
                leadName = "Dana",
                agentName = "Avi Cohen",
                officeName = "Nadlan Pro",
                phone = "050-1234567",
                website = "https://example.com",
                businessCard = "https://example.com/card",
                signature = "Avi"
            )
        )

        assertEquals(
            "Dana|Avi Cohen|Nadlan Pro|050-1234567|https://example.com|https://example.com/card|Avi",
            rendered
        )
    }

    @Test
    fun rendersRepeatedTags() {
        val rendered = TemplateTagRenderer.render(
            "{agent_name} / {agent_name} / {lead_name}",
            TemplateTagValues(agentName = "Avi", leadName = "Dana")
        )

        assertEquals("Avi / Avi / Dana", rendered)
    }

    @Test
    fun leavesUnknownTagsVisible() {
        val rendered = TemplateTagRenderer.render(
            "Hello {lead_name}, see {property_link}",
            TemplateTagValues(leadName = "Dana")
        )

        assertEquals("Hello Dana, see {property_link}", rendered)
    }

    @Test
    fun emptyValuesRenderSafely() {
        val rendered = TemplateTagRenderer.render(
            "{lead_name}{signature}",
            TemplateTagValues()
        )

        assertEquals("", rendered)
    }

    @Test
    fun rendersMixedHebrewAsciiText() {
        val rendered = TemplateTagRenderer.render(
            "שלום {lead_name}, אני {agent_name}",
            TemplateTagValues(leadName = "דנה", agentName = "אבי")
        )

        assertEquals("שלום דנה, אני אבי", rendered)
    }
}
