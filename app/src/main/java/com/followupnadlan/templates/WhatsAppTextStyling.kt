package com.followupnadlan.templates

/**
 * Pure, Android-free formatter that turns a WhatsApp-style text (the SAME string that is actually
 * sent) into styled spans, so the bubble PREVIEW is literally the sent text with styling applied —
 * one source of truth, never two diverging strings (§2).
 *
 * It recognises three inline styles, matching how WhatsApp renders them and how the design HTML
 * paints them:
 * - `*bold*`   → [SpanStyle.BOLD]
 * - `_italic_` → [SpanStyle.ITALIC] (used for the grey how-to hint line; rendered smaller + grey by
 *   the caller)
 * - bare URLs (`http://…`, `https://…`) and phone numbers → [SpanStyle.LINK] (WhatsApp link blue +
 *   underline, painted by the caller)
 *
 * The markers themselves are stripped from the visible text (WhatsApp shows the bold word, not the
 * asterisks). Line breaks are preserved by the caller — this operates on the raw string as-is.
 *
 * See WhatsAppTextStylingTest.
 */
object WhatsAppTextStyling {
    enum class SpanStyle { PLAIN, BOLD, ITALIC, LINK }

    /** One contiguous run of text with a single style. */
    data class Span(val text: String, val style: SpanStyle)

    // A run wrapped in *...* / _..._ (non-greedy, no nested marker of the same kind, single line).
    private val BOLD = Regex("""\*([^*\n]+)\*""")
    private val ITALIC = Regex("""_([^_\n]+)_""")
    // http(s) URLs, or Israeli-style phone numbers (digits, spaces, dashes, leading +).
    private val URL = Regex("""https?://\S+""")
    private val PHONE = Regex("""(?<![\w])\+?[0-9][0-9\-\s]{6,}[0-9]""")

    /**
     * Splits [raw] into ordered [Span]s. Concatenating the spans' [Span.text] yields the visible
     * text (markers removed); the raw string minus markers. Bold/italic win over link detection
     * inside their run — a link is only detected in otherwise-plain text.
     */
    fun spans(raw: String): List<Span> {
        // Pass 1: carve out bold and italic runs, leaving plain gaps to be link-scanned.
        val marked = mutableListOf<Span>()
        var index = 0
        while (index < raw.length) {
            val bold = BOLD.find(raw, index)
            val italic = ITALIC.find(raw, index)
            val next = listOfNotNull(bold, italic).minByOrNull { it.range.first }
            if (next == null) {
                if (index < raw.length) marked += Span(raw.substring(index), SpanStyle.PLAIN)
                break
            }
            if (next.range.first > index) {
                marked += Span(raw.substring(index, next.range.first), SpanStyle.PLAIN)
            }
            val style = if (next === bold) SpanStyle.BOLD else SpanStyle.ITALIC
            marked += Span(next.groupValues[1], style)
            index = next.range.last + 1
        }

        // Pass 2: within plain spans only, split out links (URLs + phones).
        return marked.flatMap { span ->
            if (span.style == SpanStyle.PLAIN) linkify(span.text) else listOf(span)
        }.filter { it.text.isNotEmpty() }
    }

    private fun linkify(text: String): List<Span> {
        val matches = (URL.findAll(text) + PHONE.findAll(text))
            .sortedBy { it.range.first }
            // Drop overlaps (a phone inside a URL): keep the earliest, skip any that start before
            // the previous one ended.
            .fold(mutableListOf<MatchResult>()) { acc, m ->
                if (acc.isEmpty() || m.range.first > acc.last().range.last) acc += m
                acc
            }
        if (matches.isEmpty()) return listOf(Span(text, SpanStyle.PLAIN))

        val out = mutableListOf<Span>()
        var cursor = 0
        for (m in matches) {
            if (m.range.first > cursor) out += Span(text.substring(cursor, m.range.first), SpanStyle.PLAIN)
            out += Span(m.value, SpanStyle.LINK)
            cursor = m.range.last + 1
        }
        if (cursor < text.length) out += Span(text.substring(cursor), SpanStyle.PLAIN)
        return out
    }
}
