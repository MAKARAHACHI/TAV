package com.followupnadlan.templates

import com.followupnadlan.profile.ContactCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EndedMessageComposerTest {
    private val card = ContactCard(fullName = "דני לוי", org = "עו\"ד מקרקעין", phone = "052-1234567")
    private val body = "תודה על השיחה! שמח שדיברנו. מצרף את הפרטים שלי:"

    @Test
    fun attachesTheCardAsTheClosingLine() {
        val composed = EndedMessageComposer.compose(body, card, attachCard = true)
        assertEquals("$body\nדני לוי · עו\"ד מקרקעין · 052-1234567", composed)
    }

    // The "מצורף" switch must change what is actually sent, not only the preview (§2).
    @Test
    fun omitsTheCardEntirelyWhenDetached() {
        val composed = EndedMessageComposer.compose(body, card, attachCard = false)
        assertEquals(body, composed)
        assertFalse(composed.contains("דני לוי"))
    }

    @Test
    fun sendsWithoutACardLineWhenTheProfileIsEmpty() {
        val composed = EndedMessageComposer.compose(body, ContactCard("", "", ""), attachCard = true)
        assertEquals(body, composed)
    }

    @Test
    fun attachesWhateverPartsExistOnAPartialProfile() {
        val partial = ContactCard(fullName = "דני לוי", org = "", phone = "052-1234567")
        val composed = EndedMessageComposer.compose(body, partial, attachCard = true)
        assertEquals("$body\nדני לוי · 052-1234567", composed)
    }
}
