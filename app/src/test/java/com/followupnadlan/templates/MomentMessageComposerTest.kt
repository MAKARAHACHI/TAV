package com.followupnadlan.templates

import com.followupnadlan.profile.ContactCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentMessageComposerTest {
    private val card = ContactCard(
        fullName = "שלמה בן ארצי",
        org = "עורך דין",
        phone = "054-5555565",
        website = "https://domain.co.il"
    )
    private val bodyWithLinks = "תודה על השיחה!\n\n${MessageComposition.WEBSITE_LABEL} https://domain.co.il"
    private val bodyWithoutLinks = "תודה על השיחה!"

    @Test
    fun cardOffKeepsOldBehaviourBodyWithLinksPlusSignature() {
        val out = MomentMessageComposer.compose(bodyWithLinks, bodyWithoutLinks, card, attachCard = false)
        // Old link line survives, one-line signature closes it, no text card.
        assertTrue(out.contains(MessageComposition.WEBSITE_LABEL))
        assertTrue(out.contains("שלמה בן ארצי · עורך דין · 054-5555565"))
        assertFalse(out.contains("לשמירה מהירה"))
    }

    @Test
    fun cardOnDropsTemplateLinkLineAndAppendsTextCard() {
        val out = MomentMessageComposer.compose(bodyWithLinks, bodyWithoutLinks, card, attachCard = true)
        // The template's own website label is suppressed (card owns the website) — appears once.
        assertFalse("template link line must be dropped when card is on", out.contains(MessageComposition.WEBSITE_LABEL))
        // Body + one-line signature still present.
        assertTrue(out.contains("תודה על השיחה!"))
        assertTrue(out.contains("שלמה בן ארצי · עורך דין · 054-5555565"))
        // Text card present.
        assertTrue(out.contains("📱 *לשמירה מהירה באנשי הקשר:*\n054-5555565"))
        // Website appears exactly once overall.
        assertEquals(1, Regex(Regex.escape("https://domain.co.il")).findAll(out).count())
    }

    @Test
    fun cardOnWithEmptyProfileFallsBackToBodyPlusSignatureOnly() {
        val empty = ContactCard("", "", "", "")
        val out = MomentMessageComposer.compose(bodyWithLinks, bodyWithoutLinks, empty, attachCard = true)
        // No name ⇒ no text card, and no signature either (nothing to sign with).
        assertEquals("תודה על השיחה!", out)
    }

    @Test
    fun templateOverloadUsesRawBodyWhenCardOn() {
        val template = MessageTemplate(
            id = "t1",
            title = "ended",
            body = "תודה על השיחה!",
            cardLink = "",
            websiteLink = "https://domain.co.il",
            role = TemplateRole.CALL_ENDED
        )
        val out = MomentMessageComposer.compose(template, card, attachCard = true)
        assertFalse(out.contains(MessageComposition.WEBSITE_LABEL))
        assertTrue(out.contains("📱 *לשמירה מהירה באנשי הקשר:*"))
    }
}
