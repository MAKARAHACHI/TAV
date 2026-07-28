package com.followupnadlan.templates

import com.followupnadlan.templates.WhatsAppTextStyling.SpanStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppTextStylingTest {
    private fun visible(raw: String) = WhatsAppTextStyling.spans(raw).joinToString("") { it.text }

    @Test
    fun stripsBoldMarkersAndMarksTheRunBold() {
        val spans = WhatsAppTextStyling.spans("שלום *עולם* שלום")
        assertEquals("שלום עולם שלום", spans.joinToString("") { it.text })
        val bold = spans.single { it.style == SpanStyle.BOLD }
        assertEquals("עולם", bold.text)
    }

    @Test
    fun marksItalicRun() {
        val spans = WhatsAppTextStyling.spans("_(לחצו על המספר)_")
        assertEquals(1, spans.size)
        assertEquals(SpanStyle.ITALIC, spans.single().style)
        assertEquals("(לחצו על המספר)", spans.single().text)
    }

    @Test
    fun detectsHttpsUrlAsLink() {
        val spans = WhatsAppTextStyling.spans("https://domain.co.il")
        assertEquals(SpanStyle.LINK, spans.single().style)
    }

    @Test
    fun detectsPhoneNumberAsLink() {
        val spans = WhatsAppTextStyling.spans("054-5555565")
        assertTrue(spans.any { it.style == SpanStyle.LINK && it.text == "054-5555565" })
    }

    @Test
    fun boldLabelThenLinkOnNextLine() {
        val spans = WhatsAppTextStyling.spans("⭐ *חוות דעת וביקורות:*\nhttps://domain.co.il")
        assertTrue(spans.any { it.style == SpanStyle.BOLD && it.text == "חוות דעת וביקורות:" })
        assertTrue(spans.any { it.style == SpanStyle.LINK && it.text == "https://domain.co.il" })
        // The full visible text keeps the newline and drops the asterisks.
        assertEquals("⭐ חוות דעת וביקורות:\nhttps://domain.co.il", spans.joinToString("") { it.text })
    }

    @Test
    fun fullCardRoundTripsToVisibleTextWithMarkersStripped() {
        val card = ContactTextCard.build(
            com.followupnadlan.profile.ContactCard(
                fullName = "שלמה בן ארצי",
                org = "עורך דין",
                phone = "054-5555565",
                website = "https://domain.co.il"
            )
        )
        val out = visible(card)
        // No stray markers survive.
        assertTrue(!out.contains("*"))
        assertTrue(!out.contains("_"))
        // Content survives.
        assertTrue(out.contains("שלמה בן ארצי"))
        assertTrue(out.contains("054-5555565"))
        assertTrue(out.contains("https://domain.co.il"))
    }

    @Test
    fun plainTextWithoutMarkersIsASinglePlainSpan() {
        val spans = WhatsAppTextStyling.spans("שלום עולם")
        assertEquals(1, spans.size)
        assertEquals(SpanStyle.PLAIN, spans.single().style)
    }
}
