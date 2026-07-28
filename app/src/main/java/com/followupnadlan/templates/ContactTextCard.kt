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
 *     ⚖️ *{name}* │ *{role}*
 *
 *     ⭐ *חוות דעת וביקורות:*
 *     {website}
 *
 *     📱 *לשמירה מהירה באנשי הקשר:*
 *     {phone}
 *
 *     _(לחצו על המספר ← הוספה לאנשי קשר)_
 *
 * §2 — never emit a placeholder or an empty labelled line:
 * - No name ⇒ a nameless card is meaningless ⇒ return "" (the card is not offered at all).
 * - Blank role ⇒ drop the ` │ *{role}*` tail; the header is just `⚖️ *{name}*`.
 * - Blank website ⇒ omit the whole "חוות דעת וביקורות" block (label + link).
 * - Blank phone ⇒ omit the whole "לשמירה מהירה" block AND the grey how-to hint (it explains how
 *   to save that number, so it is meaningless without it).
 * - Sections collapse cleanly: exactly one blank line between blocks, no trailing whitespace, no
 *   dangling emoji or label without its value.
 *
 * Pure value logic, no Android — see ContactTextCardTest.
 */
object ContactTextCard {
    /**
     * The default header emoji. There is no profession/emoji source in the profile yet, so this is
     * a single documented constant (matches the HTML) — change here to swap the profession glyph.
     */
    const val HEADER_EMOJI = "⚖️"

    const val REVIEWS_LABEL = "חוות דעת וביקורות:"
    const val SAVE_LABEL = "לשמירה מהירה באנשי הקשר:"
    const val SAVE_HINT = "(לחצו על המספר ← הוספה לאנשי קשר)"

    fun build(card: ContactCard): String {
        val name = card.fullName.trim()
        if (name.isEmpty()) return ""

        val role = card.org.trim()
        val website = card.website.trim()
        val phone = card.phone.trim()

        val blocks = mutableListOf<String>()

        // Header — name always, role only when present.
        blocks += if (role.isNotEmpty()) {
            "$HEADER_EMOJI *$name* │ *$role*"
        } else {
            "$HEADER_EMOJI *$name*"
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
