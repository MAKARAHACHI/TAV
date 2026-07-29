package com.followupnadlan.accessibility

import com.followupnadlan.profile.ContactCard
import com.followupnadlan.profile.SignatureLine
import com.followupnadlan.templates.ContactTextCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave E: the approval sheet's per-send "add/remove card" chip (`cardOn`) composes the outgoing
 * message the same way regardless of which moment opened the sheet — see the `outgoingMessage`
 * val in `MissedCallPromptScreen` (AccessibilityApp.kt), which this mirrors exactly:
 *
 *     if (cardOn) ContactTextCard.append(resolvedMessage, card) else SignatureLine.append(resolvedMessage, card)
 *
 * [composeForSend] is that same one-line rule extracted so it can be asserted without Compose —
 * the sheet itself is not re-implemented, only the pure string composition it drives (§2: the
 * bubble preview and the sent text must be built from this exact branch at both toggle states).
 */
private fun composeForSend(body: String, card: ContactCard, cardOn: Boolean): String =
    if (cardOn) ContactTextCard.append(body, card) else SignatureLine.append(body, card)

class MissedCallPromptCardToggleTest {
    private val card = ContactCard(
        fullName = "שלמה בן ארצי",
        org = "עורך דין",
        phone = "054-5555565",
        website = "https://domain.co.il"
    )
    // A pure body: as `activeBody`/`draft` are in the sheet — already stripped of any card/signature.
    private val pureBody = "תודה על השיחה, נדבר בקרוב!"

    @Test
    fun cardOnAppendsTextCardAndNoStandaloneSignature() {
        val out = composeForSend(pureBody, card, cardOn = true)
        assertTrue(out.contains(pureBody))
        assertTrue("card block must be present", out.contains("📱 *לשמירה מהירה באנשי הקשר:*\n054-5555565"))
        assertFalse(
            "card ON must not also carry the standalone one-line signature",
            out.contains(SignatureLine.render(card))
        )
    }

    @Test
    fun cardOffAppendsSignatureAndNoTextCard() {
        val out = composeForSend(pureBody, card, cardOn = false)
        assertTrue(out.contains(pureBody))
        assertTrue(out.contains(SignatureLine.render(card)))
        assertFalse("card OFF must not carry the text card block", out.contains("לשמירה מהירה"))
        assertFalse(out.contains("📱"))
    }

    @Test
    fun websiteAppearsExactlyOnceWhenCardIsOnEvenIfBodyHasItsOwnLink() {
        // Mirrors the sheet's fallbackBody/picker rule: when cardOn the RAW body (no template link
        // line) is used, so the card's own website line is the only occurrence. This asserts the
        // downstream guarantee holds for composeForSend given such a raw body.
        val out = composeForSend(pureBody, card, cardOn = true)
        assertEquals(1, Regex(Regex.escape("https://domain.co.il")).findAll(out).count())
    }

    @Test
    fun togglingOnOffOnIsIdempotentForAFixedPureBody() {
        // The chip flips `cardOn` only — the underlying pure body (`draft`/`activeBody`) never
        // changes, so composing ON, then OFF, then ON again from the SAME body must reproduce the
        // exact same string both times it is ON (and the same string both times it is OFF).
        val on1 = composeForSend(pureBody, card, cardOn = true)
        val off = composeForSend(pureBody, card, cardOn = false)
        val on2 = composeForSend(pureBody, card, cardOn = true)
        assertEquals("ON -> OFF -> ON must return to the identical ON string", on1, on2)
        assertFalse("the OFF string must differ from the ON string", off == on1)
    }

    @Test
    fun cardUnavailableWhenProfileHasNoName() {
        // Hidden-toggle precondition: `cardAvailable = cardText.isNotBlank()`. A nameless profile
        // can never produce a card, so the chip must not be offered regardless of cardOn's seed.
        val empty = ContactCard("", "", "", "")
        assertTrue(ContactTextCard.build(empty).isBlank())
        // Even if cardOn were somehow true, composing with an empty card degrades to signature-less
        // plain body (nothing truthful to append) — never a broken/placeholder card block.
        val out = composeForSend(pureBody, empty, cardOn = true)
        assertEquals(pureBody, out)
    }
}
