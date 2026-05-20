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
}
