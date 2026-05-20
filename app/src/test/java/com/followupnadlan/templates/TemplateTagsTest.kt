package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateTagsTest {
    @Test
    fun supportedTagKeysStayUnchanged() {
        val keys = TemplateTags.supported.map { it.key }

        assertEquals(
            listOf(
                "{lead_name}",
                "{agent_name}",
                "{office_name}",
                "{phone}",
                "{website}",
                "{business_card}",
                "{signature}",
                "{property_name}",
                "{property_link}"
            ),
            keys
        )
    }

    @Test
    fun supportedTagsShowHebrewLabels() {
        val labelsByKey = TemplateTags.supported.associate { it.key to it.label }

        assertEquals("שם הלקוח", labelsByKey["{lead_name}"])
        assertEquals("שם הסוכן", labelsByKey["{agent_name}"])
        assertEquals("שם המשרד", labelsByKey["{office_name}"])
        assertEquals("טלפון הסוכן", labelsByKey["{phone}"])
        assertEquals("אתר", labelsByKey["{website}"])
        assertEquals("כרטיס ביקור", labelsByKey["{business_card}"])
        assertEquals("חתימה", labelsByKey["{signature}"])
        assertEquals("שם הנכס", labelsByKey["{property_name}"])
        assertEquals("קישור לנכס", labelsByKey["{property_link}"])
    }

    @Test
    fun insertsTagAtCursor() {
        val result = TemplateTagInsertionLogic.insertTag(
            text = "hello  world",
            selectionStart = 6,
            selectionEnd = 6,
            tag = "{agent_name}"
        )

        assertEquals("hello {agent_name} world", result.text)
        assertEquals(18, result.cursorPosition)
    }

    @Test
    fun replacesSelectedTextWithTag() {
        val result = TemplateTagInsertionLogic.insertTag(
            text = "hello selected world",
            selectionStart = 6,
            selectionEnd = 14,
            tag = "{property_link}"
        )

        assertEquals("hello {property_link} world", result.text)
        assertEquals(21, result.cursorPosition)
    }

    @Test
    fun handlesReversedSelectionAndMovesCursorAfterTag() {
        val result = TemplateTagInsertionLogic.insertTag(
            text = "abc def",
            selectionStart = 7,
            selectionEnd = 4,
            tag = "{lead_name}"
        )

        assertEquals("abc {lead_name}", result.text)
        assertEquals(15, result.cursorPosition)
    }
}
