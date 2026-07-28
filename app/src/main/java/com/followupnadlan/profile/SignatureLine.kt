package com.followupnadlan.profile

/**
 * The one-line signature appended to the end of BOTH the missed and the ended message:
 *
 *     {שם} · {עיסוק} · {טלפון}
 *
 * This is a *signature, not content*. Repeating it across two messages to the same person on
 * the same day reads as professional, not robotic — so there is deliberately no skip logic.
 *
 * §2 — the hard rule: a placeholder must never reach a client. When the profile is empty or
 * partial we drop the missing parts, and when nothing is left we send the message with no
 * signature at all rather than emit "{שם}" or any other token.
 *
 * Pure value logic, no Android — see SignatureLineTest.
 */
object SignatureLine {
    const val SEPARATOR = " · "

    /**
     * The signature for [card], or an empty string when there is nothing truthful to sign with.
     * Parts are dropped individually, so a name-only profile still signs with the name.
     */
    fun render(card: ContactCard): String =
        listOf(card.fullName, card.org, card.phone)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(SEPARATOR)

    fun render(profile: MyDetailsProfile): String = render(ContactCard.fromProfile(profile))

    /**
     * [body] with the signature appended on its own line. When the signature is empty the body
     * is returned untouched — never with a dangling separator or an empty trailing line.
     */
    fun append(body: String, card: ContactCard): String {
        val signature = render(card)
        val trimmedBody = body.trim()
        return when {
            signature.isEmpty() -> trimmedBody
            trimmedBody.isEmpty() -> signature
            else -> "$trimmedBody\n$signature"
        }
    }

    /**
     * Removes the trailing signature line (and its separator) from [message] if present, so a call
     * site that receives an already-composed message can re-derive the pure body and re-append the
     * signature through one path — keeping preview == sent and never doubling the signature (§2).
     * Returns [message] trimmed when no signature is present or the profile is empty.
     */
    fun removeFrom(message: String, card: ContactCard): String {
        val signature = render(card)
        val trimmed = message.trim()
        if (signature.isEmpty()) return trimmed
        return when {
            trimmed == signature -> ""
            trimmed.endsWith("\n$signature") -> trimmed.removeSuffix("\n$signature").trim()
            else -> trimmed
        }
    }
}
