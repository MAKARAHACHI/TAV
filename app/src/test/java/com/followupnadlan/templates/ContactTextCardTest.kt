package com.followupnadlan.templates

import com.followupnadlan.profile.ContactCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactTextCardTest {
    private val full = ContactCard(
        fullName = "שלמה בן ארצי",
        org = "עורך דין ומגשר",
        phone = "054-5555565",
        website = "https://domain.co.il"
    )

    @Test
    fun fullProfileBuildsEveryBlockNoLeadingEmojiByDefault() {
        val expected = listOf(
            "*שלמה בן ארצי* │ *עורך דין ומגשר*",
            "⭐ *חוות דעת וביקורות:*\nhttps://domain.co.il",
            "📱 *לשמירה מהירה באנשי הקשר:*\n054-5555565",
            "_(לחצו על המספר ← הוספה לאנשי קשר)_"
        ).joinToString("\n\n")
        assertEquals(expected, ContactTextCard.build(full))
    }

    @Test
    fun nameOnlyIsJustTheHeaderNoRoleTail() {
        val card = ContactCard(fullName = "שלמה בן ארצי", org = "", phone = "", website = "")
        assertEquals("*שלמה בן ארצי*", ContactTextCard.build(card))
    }

    @Test
    fun emptyEmojiHeaderStartsAtNameWithNoLeadingSpace() {
        val card = ContactCard(fullName = "דני", org = "", phone = "", website = "", emoji = "")
        val out = ContactTextCard.build(card)
        assertEquals("*דני*", out)
        assertFalse("no leading space", out.startsWith(" "))
    }

    @Test
    fun emojiPresentNoRolePrefixesTheHeader() {
        val card = ContactCard(fullName = "דני", org = "", phone = "", website = "", emoji = "🏠")
        assertEquals("🏠 *דני*", ContactTextCard.build(card))
    }

    @Test
    fun emojiPresentWithRolePrefixesTheHeader() {
        val card = ContactCard(fullName = "דני", org = "מתווך", phone = "", website = "", emoji = "🏠")
        assertEquals("🏠 *דני* │ *מתווך*", ContactTextCard.build(card))
    }

    @Test
    fun namePlusPhoneNoWebsiteOmitsReviewsBlockKeepsHint() {
        val card = ContactCard(fullName = "שלמה בן ארצי", org = "עורך דין", phone = "054-5555565", website = "")
        val out = ContactTextCard.build(card)
        assertFalse(out.contains("חוות דעת"))
        assertTrue(out.contains("📱 *לשמירה מהירה באנשי הקשר:*\n054-5555565"))
        assertTrue(out.contains("_(לחצו על המספר ← הוספה לאנשי קשר)_"))
    }

    @Test
    fun websiteWithoutPhoneOmitsSaveBlockAndHint() {
        val card = ContactCard(fullName = "שלמה בן ארצי", org = "", phone = "", website = "https://domain.co.il")
        val out = ContactTextCard.build(card)
        assertTrue(out.contains("⭐ *חוות דעת וביקורות:*\nhttps://domain.co.il"))
        assertFalse(out.contains("לשמירה מהירה"))
        assertFalse(out.contains("לחצו על המספר"))
    }

    @Test
    fun missingNameReturnsEmptyCard() {
        assertEquals("", ContactTextCard.build(ContactCard(fullName = "", org = "עו\"ד", phone = "054-1", website = "x")))
        assertEquals("", ContactTextCard.build(ContactCard(fullName = "   ", org = "עו\"ד", phone = "054-1", website = "x")))
    }

    @Test
    fun neverEmitsABoldLabelWithoutItsValue() {
        // A blank website must not leave a dangling "⭐ *...*" label, and a blank phone must not
        // leave a dangling "📱 *...*" label or hint.
        val noWebsite = ContactTextCard.build(full.copy(website = ""))
        assertFalse(noWebsite.contains("⭐"))
        val noPhone = ContactTextCard.build(full.copy(phone = ""))
        assertFalse(noPhone.contains("📱"))
        assertFalse(noPhone.contains("לחצו על המספר"))
    }

    @Test
    fun collapsesCleanlyNoDoubleBlankLinesNoTrailingSpace() {
        val out = ContactTextCard.build(full)
        assertFalse("no triple newline", out.contains("\n\n\n"))
        assertEquals("no trailing whitespace", out, out.trimEnd())
        out.lines().forEach { line ->
            assertEquals("no trailing space on line: [$line]", line, line.trimEnd())
        }
    }

    @Test
    fun appendSeparatesBodyFromCardWithABlankLine() {
        val body = "תודה על השיחה!"
        assertEquals("$body\n\n${ContactTextCard.build(full)}", ContactTextCard.append(body, full))
    }

    @Test
    fun appendReturnsBodyUntouchedWhenCardIsEmpty() {
        val body = "תודה על השיחה!"
        assertEquals(body, ContactTextCard.append(body, ContactCard(fullName = "", org = "", phone = "")))
    }

    @Test
    fun removeFromStripsTheTrailingCardSoItCanBeReAppendedOnce() {
        val body = "תודה על השיחה!"
        val composed = ContactTextCard.append(body, full)
        assertEquals(body, ContactTextCard.removeFrom(composed, full))
        // Round trip is stable: strip then append reproduces the composed message exactly.
        assertEquals(composed, ContactTextCard.append(ContactTextCard.removeFrom(composed, full), full))
    }

    @Test
    fun removeFromLeavesAMessageWithoutACardUntouched() {
        val body = "תודה על השיחה!"
        assertEquals(body, ContactTextCard.removeFrom(body, full))
    }

    @Test
    fun removeFromWithEmptyCardReturnsTrimmedMessage() {
        val body = "  תודה על השיחה!  "
        assertEquals("תודה על השיחה!", ContactTextCard.removeFrom(body, ContactCard("", "", "")))
    }
}
