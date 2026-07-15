package com.followupnadlan.templates

/**
 * Builds the final message text that is actually sent: the template [MessageTemplate.body]
 * followed by optional link lines. The links are separate fields (not part of the body) so
 * the user edits the message text once and the links are appended consistently.
 *
 * Layout (blank line separates the body from the links block; links sit on their own lines):
 *
 *     <body>
 *
 *     הכרטיס הדיגיטלי שלי: <cardLink>
 *     האתר שלי: <websiteLink>
 *
 * Blank links are omitted entirely. Pure logic, no Android — see MessageCompositionTest.
 */
object MessageComposition {
    const val CARD_LABEL = "הכרטיס הדיגיטלי שלי:"
    const val WEBSITE_LABEL = "האתר שלי:"

    fun build(body: String, cardLink: String, websiteLink: String): String {
        val sections = mutableListOf<String>()

        val trimmedBody = body.trim()
        if (trimmedBody.isNotEmpty()) {
            sections.add(trimmedBody)
        }

        val linkLines = buildList {
            val card = cardLink.trim()
            if (card.isNotEmpty()) add("$CARD_LABEL $card")
            val website = websiteLink.trim()
            if (website.isNotEmpty()) add("$WEBSITE_LABEL $website")
        }
        if (linkLines.isNotEmpty()) {
            sections.add(linkLines.joinToString("\n"))
        }

        return sections.joinToString("\n\n")
    }

    fun build(template: MessageTemplate): String =
        build(template.body, template.cardLink, template.websiteLink)
}
