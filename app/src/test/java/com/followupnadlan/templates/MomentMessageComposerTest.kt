package com.followupnadlan.templates

import com.followupnadlan.profile.ContactCard
import com.followupnadlan.profile.SignatureLine
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
    // A name-only profile: the one-line signature is just the name, and the text card is the name only.
    private val nameOnlyCard = ContactCard(fullName = "שלמה בן ארצי", org = "", phone = "", website = "")
    private val bodyWithLinks = "תודה על השיחה!\n\n${MessageComposition.WEBSITE_LABEL} https://domain.co.il"
    private val bodyWithoutLinks = "תודה על השיחה!"

    @Test
    fun cardOffKeepsOldBehaviourBodyWithLinksPlusSignature() {
        val out = MomentMessageComposer.compose(bodyWithLinks, bodyWithoutLinks, card, attachCard = false)
        // Old link line survives, one-line signature closes it, no text card.
        assertTrue(out.contains(MessageComposition.WEBSITE_LABEL))
        assertTrue(out.contains(SignatureLine.render(card)))
        assertEquals("שלמה בן ארצי · עורך דין · 054-5555565", SignatureLine.render(card))
        assertFalse(out.contains("לשמירה מהירה"))
    }

    @Test
    fun cardOffNameOnlyStillAppendsTheNameSignatureAndNoTextCard() {
        val out = MomentMessageComposer.compose(bodyWithoutLinks, bodyWithoutLinks, nameOnlyCard, attachCard = false)
        // Signature = just the name; no formatted text card at all.
        assertTrue(out.contains("שלמה בן ארצי"))
        assertFalse(out.contains("לשמירה מהירה"))
        assertFalse(out.contains("📱"))
    }

    @Test
    fun cardOnDropsTemplateLinkLineAndTheOneLineSignatureAndAppendsTextCard() {
        val out = MomentMessageComposer.compose(bodyWithLinks, bodyWithoutLinks, card, attachCard = true)
        // The template's own website label is suppressed (card owns the website) — appears once.
        assertFalse("template link line must be dropped when card is on", out.contains(MessageComposition.WEBSITE_LABEL))
        // Body still present.
        assertTrue(out.contains("תודה על השיחה!"))
        // The one-line signature is REPLACED by the card — it must NOT appear as a standalone line.
        assertFalse(
            "card ON ⇒ the standalone one-line signature must be dropped",
            out.contains(SignatureLine.render(card))
        )
        // Text card present (name/role/phone live here instead).
        assertTrue(out.contains("📱 *לשמירה מהירה באנשי הקשר:*\n054-5555565"))
        assertTrue(out.contains("שלמה בן ארצי"))
        // Website appears exactly once overall.
        assertEquals(1, Regex(Regex.escape("https://domain.co.il")).findAll(out).count())
    }

    @Test
    fun cardOnNameOnlyDropsSignatureAndShowsNameOnlyCard() {
        val out = MomentMessageComposer.compose(bodyWithoutLinks, bodyWithoutLinks, nameOnlyCard, attachCard = true)
        // Body present, name present (via the card header), but NOT as the standalone signature line.
        assertTrue(out.contains("תודה על השיחה!"))
        assertTrue(out.contains("שלמה בן ארצי"))
        // Name-only card has no phone block; the composed message must not carry the save-hint block.
        assertFalse(out.contains("לשמירה מהירה"))
        // The card header owns the name — verify it renders as the bold header, not a bare signature.
        assertTrue(out.contains("*שלמה בן ארצי*"))
    }

    @Test
    fun cardOnWithEmptyProfileFallsBackToBodyOnly() {
        val empty = ContactCard("", "", "", "")
        val out = MomentMessageComposer.compose(bodyWithLinks, bodyWithoutLinks, empty, attachCard = true)
        // No name ⇒ no text card, and no signature either (nothing to sign with).
        assertEquals("תודה על השיחה!", out)
    }

    // FIX 5 / §2: the sheet's saved-template picker chooses a variant body with the SAME rule the
    // main sheet path uses — raw body when the card is ON (card owns the website), link lines when
    // OFF. This models that selection to guard against the website double-printing regression.
    private fun pickerVariantBody(template: MessageTemplate, showCard: Boolean): String =
        if (showCard) template.body.trim() else MessageComposition.build(template)

    @Test
    fun pickerVariantCardOnUsesRawBodyAndWebsiteAppearsOnce() {
        val template = MessageTemplate(
            id = "t1",
            title = "ended",
            body = "תודה על השיחה!",
            cardLink = "",
            websiteLink = "https://domain.co.il",
            role = TemplateRole.CALL_ENDED
        )
        val picked = pickerVariantBody(template, showCard = true)
        // Raw body only — no template link line.
        assertFalse(picked.contains(MessageComposition.WEBSITE_LABEL))
        // What is actually sent: picked body + text card. Website must appear exactly once.
        val out = ContactTextCard.append(picked, card)
        assertEquals(1, Regex(Regex.escape("https://domain.co.il")).findAll(out).count())
    }

    @Test
    fun pickerVariantCardOffKeepsLinkLine() {
        val template = MessageTemplate(
            id = "t1",
            title = "ended",
            body = "תודה על השיחה!",
            cardLink = "",
            websiteLink = "https://domain.co.il",
            role = TemplateRole.CALL_ENDED
        )
        val picked = pickerVariantBody(template, showCard = false)
        // Card OFF ⇒ unchanged: the template's own link line is present.
        assertTrue(picked.contains(MessageComposition.WEBSITE_LABEL))
        assertTrue(picked.contains("https://domain.co.il"))
    }

    @Test
    fun templateOverloadUsesRawBodyWhenCardOnAndDropsSignature() {
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
        assertFalse(out.contains(SignatureLine.render(card)))
        assertTrue(out.contains("📱 *לשמירה מהירה באנשי הקשר:*"))
    }
}
