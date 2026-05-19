package com.followupnadlan.templates

data class TemplateTagValues(
    val leadName: String = "",
    val agentName: String = "",
    val officeName: String = "",
    val phone: String = "",
    val website: String = "",
    val businessCard: String = "",
    val signature: String = ""
)

object TemplateTagRenderer {
    fun render(template: String, values: TemplateTagValues): String {
        val replacements = mapOf(
            "{lead_name}" to values.leadName,
            "{agent_name}" to values.agentName,
            "{office_name}" to values.officeName,
            "{phone}" to values.phone,
            "{website}" to values.website,
            "{business_card}" to values.businessCard,
            "{signature}" to values.signature
        )

        return replacements.entries.fold(template) { rendered, (tag, value) ->
            rendered.replace(tag, value)
        }
    }
}
