package com.followupnadlan.templates

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

/**
 * Persists the user's message templates as a dynamic list (add / edit / delete),
 * replacing the old fixed set of three ids each stored under its own key.
 *
 * Storage: a single key [KEY_TEMPLATES] holding all templates, one per line, fields
 * Base64-encoded and joined by [TemplateCodec.FIELD_SEPARATOR] (same codec style as the
 * recipient stores). On first read after upgrade the old per-id `template_body_<id>` values
 * are migrated in (see [TemplateMigrationLogic]) and the legacy keys removed.
 */
class TemplateStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadTemplates(): List<MessageTemplate> {
        val raw = preferences.getString(KEY_TEMPLATES, null)
        if (raw != null) {
            return TemplateCodec.decode(raw).ifEmpty { seed(SprintOneTemplates.all) }
        }
        return migrateFromLegacy()
    }

    /** Upserts [template] by id, then persists the whole list. */
    fun saveTemplate(template: MessageTemplate) {
        persist(TemplateListLogic.upsert(loadTemplates(), template))
    }

    /** Appends a new template with a fresh id and returns it. */
    fun addTemplate(
        title: String,
        body: String,
        cardLink: String = "",
        websiteLink: String = ""
    ): MessageTemplate {
        val template = MessageTemplate(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            cardLink = cardLink,
            websiteLink = websiteLink
        )
        persist(loadTemplates() + template)
        return template
    }

    /** Removes the template unless it is the only one left (the engine always needs one). */
    fun deleteTemplate(id: String) {
        persist(TemplateListLogic.delete(loadTemplates(), id))
    }

    private fun migrateFromLegacy(): List<MessageTemplate> {
        val oldBodiesById = SprintOneTemplates.all.associate { template ->
            template.id to preferences.getString(legacyBodyKey(template.id), null)
        }
        val migrated = TemplateMigrationLogic.migrate(SprintOneTemplates.all, oldBodiesById)
        val editor = preferences.edit()
        editor.putString(KEY_TEMPLATES, TemplateCodec.encode(migrated))
        SprintOneTemplates.all.forEach { editor.remove(legacyBodyKey(it.id)) }
        editor.commit()
        return migrated
    }

    private fun seed(templates: List<MessageTemplate>): List<MessageTemplate> {
        persist(templates)
        return templates
    }

    private fun persist(templates: List<MessageTemplate>) {
        preferences.edit()
            .putString(KEY_TEMPLATES, TemplateCodec.encode(templates))
            .commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "message_templates"
        const val KEY_TEMPLATES = "templates"

        fun legacyBodyKey(templateId: String): String = "template_body_$templateId"
    }
}

/** Pure add/edit/delete list operations, kept separate for tests. */
internal object TemplateListLogic {
    fun upsert(templates: List<MessageTemplate>, template: MessageTemplate): List<MessageTemplate> =
        if (templates.any { it.id == template.id }) {
            templates.map { if (it.id == template.id) template else it }
        } else {
            templates + template
        }

    /** Deletes by id, but never removes the last remaining template. */
    fun delete(templates: List<MessageTemplate>, id: String): List<MessageTemplate> =
        if (templates.size <= 1) templates else templates.filterNot { it.id == id }
}

/**
 * One-time migration from the old per-id bodies to the dynamic list. Seeds the three
 * accessibility defaults, applying any user-customized body that is not a legacy default
 * (legacy default bodies are dropped so the new copy shows — see [LegacyTemplateMigration]).
 */
internal object TemplateMigrationLogic {
    fun migrate(
        defaults: List<MessageTemplate>,
        oldBodiesById: Map<String, String?>
    ): List<MessageTemplate> =
        TemplateStoreLogic.applySavedBodies(
            builtInTemplates = defaults,
            savedBodiesById = oldBodiesById.mapValues { (_, body) ->
                if (LegacyTemplateMigration.isLegacyDefaultBody(body)) null else body
            }
        )
}

/** Serializes a template list to a single string, one template per line. */
internal object TemplateCodec {
    private const val LINE_SEPARATOR = "\n"
    const val FIELD_SEPARATOR = "|"

    fun encode(templates: List<MessageTemplate>): String =
        templates.joinToString(separator = LINE_SEPARATOR) { template ->
            listOf(
                template.id,
                template.title,
                template.body,
                template.cardLink,
                template.websiteLink
            ).joinToString(FIELD_SEPARATOR) { encodeValue(it) }
        }

    fun decode(raw: String): List<MessageTemplate> =
        raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull(::decodeLine)
            .toList()

    private fun decodeLine(line: String): MessageTemplate? {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size < 3) return null
        val id = decodeValue(fields[0])
        if (id.isBlank()) return null
        return MessageTemplate(
            id = id,
            title = decodeValue(fields[1]),
            body = decodeValue(fields[2]),
            cardLink = fields.getOrNull(3)?.let(::decodeValue).orEmpty(),
            websiteLink = fields.getOrNull(4)?.let(::decodeValue).orEmpty()
        )
    }

    private fun encodeValue(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeValue(value: String): String =
        runCatching { String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8) }.getOrDefault("")
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
