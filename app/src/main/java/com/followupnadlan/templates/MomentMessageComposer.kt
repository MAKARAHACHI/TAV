package com.followupnadlan.templates

import com.followupnadlan.profile.ContactCard

/**
 * Single source of truth for the FULL outgoing message of a moment — the exact string that is both
 * PREVIEWED in the bubble and SENT (§2: preview == sent). Every call site (Home preview, approval
 * sheet, ended/no-answer/missed send paths) composes through here, so the styled preview and the
 * plain-text send can never diverge.
 *
 * Layout when the card is attached:
 *
 *     <template body, WITHOUT the old link lines>
 *
 *     <formatted text business card>  (owns the name/role/website/phone presentation)
 *
 * When the card is on the card REPLACES the one-line signature — the name/role/phone already live
 * in the card, so the separate `{name}·{role}·{phone}` line is dropped to avoid saying it twice.
 *
 * When the card is NOT attached, behaviour is exactly as before: the template body with its own
 * link lines ([MessageComposition]) plus the one-line signature.
 *
 * Website de-duplication: when the card is on it OWNS the website/phone lines, so the template's
 * own `cardLink`/`websiteLink` append ([MessageComposition]) is suppressed for that moment; the raw
 * template body is used instead. When the card is off, the old link-append is kept unchanged.
 *
 * Pure logic, no Android — see MomentMessageComposerTest.
 */
object MomentMessageComposer {
    /**
     * @param bodyWithLinks the body as [MessageComposition] would render it (body + link lines) —
     *   used when the card is OFF.
     * @param bodyWithoutLinks the raw template body (no link lines) — used when the card is ON so
     *   the website is not shown twice.
     * @param card the profile-derived contact card.
     * @param attachCard whether this moment's "צירוף כרטיס ביקור" toggle is on.
     */
    fun compose(
        bodyWithLinks: String,
        bodyWithoutLinks: String,
        card: ContactCard,
        attachCard: Boolean
    ): String {
        if (!attachCard) {
            // Old behaviour, untouched: body (+ link lines) + one-line signature.
            return EndedMessageComposer.compose(bodyWithLinks, card, attachCard = false)
                .let { withSignature(it, card) }
        }
        // Card on: raw body (no link lines), then the text card — which REPLACES the one-line
        // signature (name/role/phone live in the card). No signature is appended here.
        return ContactTextCard.append(bodyWithoutLinks.trim(), card)
    }

    /** Convenience for the common template-driven case. */
    fun compose(template: MessageTemplate, card: ContactCard, attachCard: Boolean): String =
        compose(
            bodyWithLinks = MessageComposition.build(template),
            bodyWithoutLinks = template.body,
            card = card,
            attachCard = attachCard
        )

    private fun withSignature(body: String, card: ContactCard): String =
        com.followupnadlan.profile.SignatureLine.append(body, card)
}
