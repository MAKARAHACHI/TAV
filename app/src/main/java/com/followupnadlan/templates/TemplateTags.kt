package com.followupnadlan.templates

data class TemplateTagOption(
    val key: String,
    val label: String
)

data class TemplateTagInsertion(
    val text: String,
    val cursorPosition: Int
)

object TemplateTags {
    val supported = listOf(
        TemplateTagOption("{lead_name}", "שם הלקוח"),
        TemplateTagOption("{agent_name}", "שם הסוכן"),
        TemplateTagOption("{office_name}", "שם המשרד"),
        TemplateTagOption("{phone}", "טלפון הסוכן"),
        TemplateTagOption("{website}", "אתר"),
        TemplateTagOption("{business_card}", "כרטיס ביקור"),
        TemplateTagOption("{signature}", "חתימה"),
        TemplateTagOption("{property_name}", "שם הנכס"),
        TemplateTagOption("{property_link}", "קישור לנכס")
    )
}

object TemplateTagInsertionLogic {
    fun insertTag(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        tag: String
    ): TemplateTagInsertion {
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(0, text.length)
        val rangeStart = minOf(start, end)
        val rangeEnd = maxOf(start, end)
        val updatedText = text.replaceRange(rangeStart, rangeEnd, tag)

        return TemplateTagInsertion(
            text = updatedText,
            cursorPosition = rangeStart + tag.length
        )
    }
}
