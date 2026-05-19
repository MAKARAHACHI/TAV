package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateTagRendererTest {
    @Test
    fun rendersAllSupportedTags() {
        val rendered = TemplateTagRenderer.render(
            "{lead_name}|{agent_name}|{office_name}|{phone}|{website}|{business_card}|{signature}|{property_name}|{property_link}",
            TemplateTagValues(
                leadName = "Dana",
                agentName = "Avi Cohen",
                officeName = "Nadlan Pro",
                phone = "050-1234567",
                website = "https://example.com",
                businessCard = "https://example.com/card",
                signature = "Avi",
                propertyName = "HaYarkon 10",
                propertyLink = "https://example.com/property"
            )
        )

        assertEquals(
            "Dana|Avi Cohen|Nadlan Pro|050-1234567|https://example.com|https://example.com/card|Avi|HaYarkon 10|https://example.com/property",
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
            "Hello {lead_name}, see {unknown_property_tag}",
            TemplateTagValues(leadName = "Dana")
        )

        assertEquals("Hello Dana, see {unknown_property_tag}", rendered)
    }

    @Test
    fun rendersActivePropertyTags() {
        val rendered = TemplateTagRenderer.render(
            "{property_name}: {property_link}",
            TemplateTagValues(
                propertyName = "Dizengoff 12",
                propertyLink = "https://example.com/dizengoff-12"
            )
        )

        assertEquals("Dizengoff 12: https://example.com/dizengoff-12", rendered)
    }

    @Test
    fun rendersRepeatedPropertyTags() {
        val rendered = TemplateTagRenderer.render(
            "{property_name} / {property_name} / {property_link}",
            TemplateTagValues(propertyName = "Neve Tzedek", propertyLink = "https://example.com/neve")
        )

        assertEquals("Neve Tzedek / Neve Tzedek / https://example.com/neve", rendered)
    }

    @Test
    fun emptyActivePropertyValuesRenderSafely() {
        val rendered = TemplateTagRenderer.render(
            "{property_name}{property_link}",
            TemplateTagValues()
        )

        assertEquals("", rendered)
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
