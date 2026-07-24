package com.followupnadlan.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardBuilderTest {

    private fun card(fullName: String = "דניאל כהן", org: String = "נדל\"ן", phone: String = "0501234567") =
        ContactCard(fullName = fullName, org = org, phone = phone)

    @Test
    fun buildsWellFormedVCardWithCrlfSeparators() {
        val vcard = VCardBuilder.build(card())!!
        assertEquals(
            "BEGIN:VCARD\r\n" +
                "VERSION:3.0\r\n" +
                "N:דניאל כהן;;;;\r\n" +
                "FN:דניאל כהן\r\n" +
                "ORG:נדל\"ן\r\n" +
                "TEL;TYPE=CELL:0501234567\r\n" +
                "END:VCARD\r\n",
            vcard
        )
    }

    @Test
    fun emitsNFieldWithFullNameInFamilyPositionRightAfterVersion() {
        val vcard = VCardBuilder.build(card())!!
        // N: present, full name in the family position, given/middle/prefix/suffix empty.
        assertTrue(vcard.contains("N:דניאל כהן;;;;\r\n"))
        // N: sits immediately after VERSION (required for WhatsApp add-to-contacts).
        assertTrue(vcard.contains("VERSION:3.0\r\nN:דניאל כהן;;;;\r\n"))
    }

    @Test
    fun escapesSpecialCharsInNFieldSoItCannotInjectAProperty() {
        val vcard = VCardBuilder.build(card(fullName = "a;b,c\\d"))!!
        assertTrue(vcard.contains("N:a\\;b\\,c\\\\d;;;;\r\n"))
    }

    @Test
    fun endsWithEndVcardCrlf() {
        val vcard = VCardBuilder.build(card())!!
        assertTrue(vcard.endsWith("END:VCARD\r\n"))
    }

    @Test
    fun everyLineIsCrlfTerminated() {
        val vcard = VCardBuilder.build(card())!!
        // No lone LF that isn't preceded by CR.
        vcard.forEachIndexed { i, c ->
            if (c == '\n') assertTrue("lone LF at $i", i > 0 && vcard[i - 1] == '\r')
        }
    }

    @Test
    fun omitsOrgWhenBlank() {
        assertFalse(VCardBuilder.build(card(org = ""))!!.contains("ORG:"))
    }

    @Test
    fun omitsOrgWhenWhitespaceOnly() {
        assertFalse(VCardBuilder.build(card(org = "   "))!!.contains("ORG:"))
    }

    @Test
    fun returnsNullWhenNameMissing() {
        assertNull(VCardBuilder.build(card(fullName = "")))
    }

    @Test
    fun returnsNullWhenPhoneMissing() {
        assertNull(VCardBuilder.build(card(phone = "")))
    }

    @Test
    fun returnsNullWhenNameIsWhitespace() {
        assertNull(VCardBuilder.build(card(fullName = "   ")))
    }

    @Test
    fun escapesAllSpecialCharactersInOneValue() {
        val vcard = VCardBuilder.build(card(fullName = "a\\b,c;d"))!!
        assertTrue(vcard.contains("FN:a\\\\b\\,c\\;d\r\n"))
    }

    @Test
    fun escapesNewlineSoItCannotInjectAProperty() {
        // A newline in the name must NOT create a new physical vCard line.
        val vcard = VCardBuilder.build(card(fullName = "Evil\nTEL;TYPE=CELL:666"))!!
        assertTrue(vcard.contains("FN:Evil\\nTEL\\;TYPE=CELL:666\r\n"))
        // Exactly the seven structural lines (BEGIN, VERSION, N, FN, ORG, TEL, END) — no injected one.
        assertEquals(7, vcard.split("\r\n").filter { it.isNotEmpty() }.size)
    }

    @Test
    fun collapsesCrlfInValueToSingleEscapedNewline() {
        val vcard = VCardBuilder.build(card(org = "line1\r\nline2"))!!
        assertTrue(vcard.contains("ORG:line1\\nline2\r\n"))
    }

    @Test
    fun collapsesLoneCrInValueToEscapedNewline() {
        val vcard = VCardBuilder.build(card(org = "line1\rline2"))!!
        assertTrue(vcard.contains("ORG:line1\\nline2\r\n"))
    }

    @Test
    fun preservesHebrewQuotesEmojiAndRtl() {
        val vcard = VCardBuilder.build(card(fullName = "משה \"אבי\" 🏠", org = "משרד עברית"))!!
        assertTrue(vcard.contains("FN:משה \"אבי\" 🏠\r\n"))
        assertTrue(vcard.contains("ORG:משרד עברית\r\n"))
    }

    @Test
    fun trimsSurroundingWhitespaceInValues() {
        val vcard = VCardBuilder.build(card(fullName = "  דניאל  ", phone = "  0501234567  "))!!
        assertTrue(vcard.contains("FN:דניאל\r\n"))
        assertTrue(vcard.contains("TEL;TYPE=CELL:0501234567\r\n"))
    }
}
