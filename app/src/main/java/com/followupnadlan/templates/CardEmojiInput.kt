package com.followupnadlan.templates

/**
 * Pure, testable validation for the OPTIONAL profile "card emoji" field ([ContactCard.emoji]).
 *
 * The field is free-text: the user types or pastes an emoji. [normalize] reduces raw input to at
 * most a single leading emoji glyph, dropping any trailing text.
 *
 * Rule (pragmatic, documented — no external emoji library):
 * - Trim the input; if empty → "".
 * - Take the FIRST Unicode code point of the trimmed input (one Kotlin "char sequence unit" via
 *   code-point iteration, so surrogate-pair emoji like 🏠 are kept whole).
 * - Keep it ONLY if that first code point is NOT a plain letter or digit and NOT ASCII — i.e. it
 *   looks like a symbol/emoji rather than the start of a word. Otherwise → "".
 * - Trailing characters after the first accepted glyph are dropped ("🏠🔑" → "🏠", "🏠 דני" → "🏠").
 *
 * Consequences (asserted in CardEmojiInputTest):
 * - "" → "", "   " → ""
 * - "🏠" → "🏠", "🏠🔑" → "🏠", "🏠 דני" → "🏠"
 * - "abc" → "" (ASCII letter), "שלום" → "" (Hebrew letter — a letter, so rejected)
 * - "123" → "" (digit)
 *
 * Note: because the leading-glyph test rejects letters/digits, Hebrew (or any) leading *text* yields
 * "" — matching the product decision that the field is for an emoji, not a word.
 */
object CardEmojiInput {
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        val firstCodePoint = trimmed.codePointAt(0)
        val charCount = Character.charCount(firstCodePoint)
        val firstGlyph = trimmed.substring(0, charCount)

        val isLetterOrDigit = Character.isLetterOrDigit(firstCodePoint)
        val isAscii = firstCodePoint <= 0x7F
        return if (isLetterOrDigit || isAscii) "" else firstGlyph
    }
}
