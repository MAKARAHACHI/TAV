package com.followupnadlan.templates

import android.content.Context

class TemplateStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadTemplates(builtInTemplates: List<MessageTemplate> = SprintOneTemplates.all): List<MessageTemplate> {
        // Drop any legacy Nadlan/business saved bodies so the accessibility default shows.
        // User-customized accessibility bodies are kept (see LegacyTemplateMigration).
        cleanLegacySavedBodies(builtInTemplates)
        return TemplateStoreLogic.applySavedBodies(
            builtInTemplates = builtInTemplates,
            savedBodiesById = builtInTemplates.associate { template ->
                template.id to preferences.getString(bodyKey(template.id), null)
            }
        )
    }

    private fun cleanLegacySavedBodies(builtInTemplates: List<MessageTemplate>) {
        builtInTemplates.forEach { template ->
            val saved = preferences.getString(bodyKey(template.id), null)
            if (LegacyTemplateMigration.isLegacyDefaultBody(saved)) {
                preferences.edit().remove(bodyKey(template.id)).apply()
            }
        }
    }

    fun saveTemplate(template: MessageTemplate) {
        preferences.edit()
            .putString(bodyKey(template.id), template.body)
            .apply()
    }

    fun resetTemplate(templateId: String) {
        preferences.edit()
            .remove(bodyKey(templateId))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "message_templates"

        fun bodyKey(templateId: String): String = "template_body_$templateId"
    }
}

internal object TemplateStoreLogic {
    fun applySavedBodies(
        builtInTemplates: List<MessageTemplate>,
        savedBodiesById: Map<String, String?>
    ): List<MessageTemplate> = builtInTemplates.map { template ->
        val savedBody = savedBodiesById[template.id]
        if (savedBody == null) {
            template
        } else {
            template.copy(body = savedBody)
        }
    }
}
