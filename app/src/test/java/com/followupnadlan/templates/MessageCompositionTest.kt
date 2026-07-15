package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageCompositionTest {
    private val body = "שלום, אפשר לכתוב לי כאן."

    @Test
    fun bodyOnlyWhenNoLinks() {
        assertEquals(body, MessageComposition.build(body, "", ""))
    }

    @Test
    fun appendsCardLinkAfterBlankLine() {
        assertEquals(
            "$body\n\n${MessageComposition.CARD_LABEL} https://card.example",
            MessageComposition.build(body, "https://card.example", "")
        )
    }

    @Test
    fun appendsWebsiteLinkAfterBlankLine() {
        assertEquals(
            "$body\n\n${MessageComposition.WEBSITE_LABEL} https://site.example",
            MessageComposition.build(body, "", "https://site.example")
        )
    }

    @Test
    fun appendsBothLinksOnConsecutiveLines() {
        assertEquals(
            "$body\n\n" +
                "${MessageComposition.CARD_LABEL} https://card.example\n" +
                "${MessageComposition.WEBSITE_LABEL} https://site.example",
            MessageComposition.build(body, "https://card.example", "https://site.example")
        )
    }

    @Test
    fun blankLinksAreIgnored() {
        assertEquals(body, MessageComposition.build(body, "   ", "\n"))
    }

    @Test
    fun trimsBodyAndLinks() {
        assertEquals(
            "$body\n\n${MessageComposition.CARD_LABEL} https://card.example",
            MessageComposition.build("  $body  ", "  https://card.example  ", "")
        )
    }

    @Test
    fun emptyBodyStillEmitsLinks() {
        assertEquals(
            "${MessageComposition.CARD_LABEL} https://card.example",
            MessageComposition.build("", "https://card.example", "")
        )
    }

    @Test
    fun buildFromTemplateUsesItsFields() {
        val template = MessageTemplate(
            id = "x",
            title = "t",
            body = body,
            cardLink = "https://card.example",
            websiteLink = "https://site.example"
        )
        assertEquals(
            MessageComposition.build(body, "https://card.example", "https://site.example"),
            MessageComposition.build(template)
        )
    }
}
