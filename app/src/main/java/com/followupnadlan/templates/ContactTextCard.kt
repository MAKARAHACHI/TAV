package com.followupnadlan.templates

import com.followupnadlan.profile.ContactCard
import com.followupnadlan.profile.MyDetailsProfile

/**
 * Builds the FORMATTED TEXT business card that is rendered inside the WhatsApp bubble AND sent as
 * real text (no .vcf file). It is the multi-line block below the message body + signature, shown
 * only when a moment's "צירוף כרטיס ביקור" toggle is ON.
 *
 * Output shape (matches design-html/app-text-vcard.html), using WhatsApp `*bold*` / `_italic_`
 * markers that render bold/italic in WhatsApp and stay as plain text in SMS:
 *
 *     {emoji} *{name}* │ *{role}*
 *
 *     ⭐ *חוות דעת וביקורות:*
 *     {website}
 *
 *     📱 *לשמירה מהירה באנשי הקשר:*
 *     {phone}
 *
 *     _(לחצו על המספר ← הוספה לאנשי קשר)_
 *
 * The leading header emoji is OPTIONAL and comes from the profile ([ContactCard.emoji]). There is
 * no built-in default glyph: when the profile emoji is blank the header simply starts at `*{name}*`
 * — never a leading space and never a lone emoji.
 *
 * §2 — never emit a placeholder or an empty labelled line:
 * - No name ⇒ a nameless card is meaningless ⇒ return "" (the card is not offered at all).
 * - Blank role ⇒ drop the ` │ *{role}*` tail; the header is just `*{name}*` (or `{emoji} *{name}*`).
 * - Blank website ⇒ omit the whole "חוות דעת וביקורות" block (label + link).
 * - Blank phone ⇒ omit the whole "לשמירה מהירה" block AND the grey how-to hint (it explains how
 *   to save that number, so it is meaningless without it).
 * - Sections collapse cleanly: exactly one blank line between blocks, no trailing whitespace, no
 *   dangling emoji or label without its value.
 *
 * Pure value logic, no Android — see ContactTextCardTest.
 */
object ContactTextCard {
    const val REVIEWS_LABEL = "חוות דעת וביקורות:"
    const val SAVE_LABEL = "לשמירה מהירה באנשי הקשר:"
    const val SAVE_HINT = "(לחצו על המספר ← הוספה לאנשי קשר)"

    fun build(card: ContactCard): String {
        val name = card.fullName.trim()
        if (name.isEmpty()) return ""

        val role = card.org.trim()
        val website = card.website.trim()
        val phone = card.phone.trim()
        val emoji = card.emoji.trim()

        val blocks = mutableListOf<String>()

        // Header — name always; optional leading emoji only when set; role tail only when present.
        // Empty emoji ⇒ header starts at `*name*` (no leading space, no lone glyph).
        val prefix = if (emoji.isNotEmpty()) "$emoji " else ""
        blocks += if (role.isNotEmpty()) {
            "$prefix*$name* │ *$role*"
        } else {
            "$prefix*$name*"
        }

        // Reviews / website block — only when a website exists.
        if (website.isNotEmpty()) {
            blocks += "⭐ *$REVIEWS_LABEL*\n$website"
        }

        // Quick-save phone block + its how-to hint — only when a phone exists.
        if (phone.isNotEmpty()) {
            blocks += "📱 *$SAVE_LABEL*\n$phone"
            blocks += "_${SAVE_HINT}_"
        }

        return blocks.joinToString("\n\n")
    }

    fun build(profile: MyDetailsProfile): String = build(ContactCard.fromProfile(profile))

    /**
     * Removes the trailing text card (and its separating blank line) from [message] if present, so a
     * call site that receives an already-composed message can re-derive the body and re-append the
     * card through one path — keeping preview == sent and never doubling the card. Returns [message]
     * trimmed when no card is present or the profile has no card.
     */
    fun removeFrom(message: String, card: ContactCard): String {
        val cardText = build(card)
        val trimmed = message.trim()
        if (cardText.isEmpty()) return trimmed
        val suffix = "\n\n$cardText"
        return when {
            trimmed == cardText -> ""
            trimmed.endsWith(suffix) -> trimmed.removeSuffix(suffix).trim()
            else -> trimmed
        }
    }

    /**
     * [body] with the text card appended, separated by a blank line. When the card is empty (no
     * name) the body is returned untouched — never with a dangling blank line. Both arguments are
     * trimmed so preview and send stay identical regardless of stray whitespace.
     */
    fun append(body: String, card: ContactCard): String {
        val cardText = build(card)
        val trimmedBody = body.trim()
        return when {
            cardText.isEmpty() -> trimmedBody
            trimmedBody.isEmpty() -> cardText
            else -> "$trimmedBody\n\n$cardText"
        }
    }
}
