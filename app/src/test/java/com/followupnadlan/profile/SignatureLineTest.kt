package com.followupnadlan.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureLineTest {
    private val fullCard = ContactCard(fullName = "דני לוי", org = "עו\"ד מקרקעין", phone = "052-1234567")

    @Test
    fun rendersAllThreePartsSeparatedByMiddot() {
        assertEquals("דני לוי · עו\"ד מקרקעין · 052-1234567", SignatureLine.render(fullCard))
    }

    @Test
    fun dropsOnlyTheMissingPartsWhenProfileIsPartial() {
        val noOccupation = fullCard.copy(org = "")
        assertEquals("דני לוי · 052-1234567", SignatureLine.render(noOccupation))
    }

    @Test
    fun rendersNameOnlyWhenThatIsAllTheUserFilledIn() {
        assertEquals("דני לוי", SignatureLine.render(ContactCard("דני לוי", "", "")))
    }

    @Test
    fun rendersEmptyForAnEmptyProfile() {
        assertEquals("", SignatureLine.render(ContactCard("", "", "")))
    }

    @Test
    fun blankPartsAreTreatedAsMissingRatherThanRenderedAsSeparators() {
        assertEquals("דני לוי · 052-1234567", SignatureLine.render(ContactCard("דני לוי", "   ", "052-1234567")))
    }

    @Test
    fun appendsSignatureOnItsOwnLineAfterTheBody() {
        val result = SignatureLine.append("תודה על השיחה!", fullCard)
        assertEquals("תודה על השיחה!\nדני לוי · עו\"ד מקרקעין · 052-1234567", result)
    }

    // §2: an empty profile must send the message *without* a signature — never a placeholder
    // and never a dangling separator line.
    @Test
    fun leavesBodyUntouchedWhenProfileIsEmpty() {
        val body = "תודה שהתקשרת, אני כרגע לא פנוי."
        assertEquals(body, SignatureLine.append(body, ContactCard("", "", "")))
    }

    @Test
    fun neverEmitsAPlaceholderToken() {
        val rendered = SignatureLine.append("גוף ההודעה", ContactCard("", "", ""))
        assertTrue("signature must not leak a template token", !rendered.contains("{"))
    }

    @Test
    fun readsTheSameFieldsAsTheContactCard() {
        val profile = MyDetailsProfile(agentName = "דני לוי", officeName = "עו\"ד מקרקעין", phone = "052-1234567")
        assertEquals(SignatureLine.render(fullCard), SignatureLine.render(profile))
    }
}
