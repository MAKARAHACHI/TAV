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
        val editor = preferences.edit()
        var changed = false
        builtInTemplates.forEach { template ->
            val saved = preferences.getString(bodyKey(template.id), null)
            if (LegacyTemplateMigration.isLegacyDefaultBody(saved)) {
                editor.remove(bodyKey(template.id))
                changed = true
            }
        }
        if (changed) {
            editor.commit()
        }
    }

    fun saveTemplate(template: MessageTemplate) {
        preferences.edit()
            .putString(bodyKey(template.id), template.body)
            .commit()
    }

    fun resetTemplate(templateId: String) {
        preferences.edit()
            .remove(bodyKey(templateId))
            .commit()
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

    fun selectedTemplate(
        templates: List<MessageTemplate>,
        selectedTemplateId: String,
        defaultTemplateId: String = SprintOneTemplates.DEFAULT_ID
    ): MessageTemplate? =
        templates.firstOrNull { it.id == selectedTemplateId }
            ?: templates.firstOrNull { it.id == defaultTemplateId }
            ?: templates.firstOrNull()
}
