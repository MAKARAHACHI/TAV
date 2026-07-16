package com.followupnadlan.profile

/**
 * Builds a vCard 3.0 string from a [ContactCard]. Pure / Android-free so it is unit-testable.
 *
 * Every value is escaped before assembly to prevent property injection — a newline inside a name
 * must never open a new vCard line. Lines are CRLF-separated and the output ends with
 * `END:VCARD\r\n`, per RFC 2426. The phone is emitted raw (no E.164 normalization); `ORG` is
 * omitted entirely when blank; there is no `N:` (no reliable given/family split from one field).
 */
object VCardBuilder {
    private const val CRLF = "\r\n"

    /** Returns the vCard text, or null when the card is incomplete (no file should be written). */
    fun build(card: ContactCard): String? {
        if (!card.isComplete) return null
        val lines = buildList {
            add("BEGIN:VCARD")
            add("VERSION:3.0")
            add("FN:${escape(card.fullName.trim())}")
            if (card.org.isNotBlank()) add("ORG:${escape(card.org.trim())}")
            add("TEL;TYPE=CELL:${escape(card.phone.trim())}")
            add("END:VCARD")
        }
        return lines.joinToString(separator = CRLF, postfix = CRLF)
    }

    /** Escapes vCard special characters so a value can never inject a new property or line. */
    private fun escape(value: String): String {
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            when (val c = value[i]) {
                '\\' -> out.append("\\\\")
                ',' -> out.append("\\,")
                ';' -> out.append("\\;")
                '\r' -> {
                    out.append("\\n")
                    // Collapse a CRLF pair into a single escaped newline.
                    if (i + 1 < value.length && value[i + 1] == '\n') i++
                }
                '\n' -> out.append("\\n")
                else -> out.append(c)
            }
            i++
        }
        return out.toString()
    }
}
