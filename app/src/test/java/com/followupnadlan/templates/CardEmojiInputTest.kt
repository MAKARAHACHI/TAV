package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import org.junit.Test

class CardEmojiInputTest {
    @Test
    fun emptyStaysEmpty() {
        assertEquals("", CardEmojiInput.normalize(""))
    }

    @Test
    fun blankStaysEmpty() {
        assertEquals("", CardEmojiInput.normalize("   "))
    }

    @Test
    fun singleEmojiIsKept() {
        assertEquals("🏠", CardEmojiInput.normalize("🏠"))
    }

    @Test
    fun twoEmojiKeepsOnlyTheFirst() {
        assertEquals("🏠", CardEmojiInput.normalize("🏠🔑"))
    }

    @Test
    fun emojiFollowedByTextKeepsOnlyTheEmoji() {
        assertEquals("🏠", CardEmojiInput.normalize("🏠 דני"))
    }

    @Test
    fun surroundingWhitespaceIsTrimmedFirst() {
        assertEquals("🏠", CardEmojiInput.normalize("  🏠  "))
    }

    @Test
    fun asciiLeadingTextIsRejected() {
        assertEquals("", CardEmojiInput.normalize("abc"))
    }

    @Test
    fun hebrewLeadingTextIsRejected() {
        // First grapheme is a Hebrew letter (a letter) ⇒ rejected per the documented rule.
        assertEquals("", CardEmojiInput.normalize("שלום"))
    }

    @Test
    fun digitLeadingTextIsRejected() {
        assertEquals("", CardEmojiInput.normalize("123"))
    }
}
