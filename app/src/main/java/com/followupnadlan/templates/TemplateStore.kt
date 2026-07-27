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
            val decoded = TemplateCodec.decode(raw).ifEmpty { return seed(SprintOneTemplates.all) }
            // A role added after this install was first stored (e.g. NO_ANSWER_OUTGOING) is not in
            // the persisted list; seed its default once so the moment always has a variant. Existing
            // roles/bodies are never touched.
            val ensured = TemplateListLogic.ensureRoleDefaults(decoded, SprintOneTemplates.all)
            if (ensured.size != decoded.size) persist(ensured)
            return ensured
        }
        return migrateFromLegacy()
    }

    /** Upserts [template] by id, then persists the whole list. */
    fun saveTemplate(template: MessageTemplate) {
        persist(TemplateListLogic.upsert(loadTemplates(), template))
    }

    /**
     * Appends a new template with a fresh id and returns it — unless the role is already at the
     * [TemplateListLogic.MAX_VARIANTS_PER_ROLE] cap (Part B), in which case nothing is added and
     * null is returned.
     */
    fun addTemplate(
        title: String,
        body: String,
        cardLink: String = "",
        websiteLink: String = "",
        role: TemplateRole = TemplateRole.CALL_ENDED
    ): MessageTemplate? {
        val current = loadTemplates()
        if (!TemplateListLogic.canAddForRole(current, role)) return null
        val template = MessageTemplate(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            cardLink = cardLink,
            websiteLink = websiteLink,
            role = role
        )
        persist(current + template)
        return template
    }

    /** Removes the template unless it is the only one left (the engine always needs one). */
    fun deleteTemplate(id: String) {
        persist(TemplateListLogic.delete(loadTemplates(), id))
    }

    /**
     * Part B: removes a variant but always keeps at least one message *for that moment's role*, so
     * a moment can never be left with no message to send.
     */
    fun deleteVariant(id: String) {
        persist(TemplateListLogic.deleteWithinRole(loadTemplates(), id))
    }

    /** Part B: whether another variant may be added for [role] (max-5 cap). */
    fun canAddVariant(role: TemplateRole): Boolean =
        TemplateListLogic.canAddForRole(loadTemplates(), role)

    /** Part B: how many variants a role currently holds. */
    fun variantCount(role: TemplateRole): Int =
        TemplateListLogic.countForRole(loadTemplates(), role)

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

    companion object {
        /** Part B: maximum message variants per moment (role). */
        const val MAX_VARIANTS_PER_ROLE = TemplateListLogic.MAX_VARIANTS_PER_ROLE

        private const val PREFERENCES_NAME = "message_templates"
        private const val KEY_TEMPLATES = "templates"

        private fun legacyBodyKey(templateId: String): String = "template_body_$templateId"
    }
}

/** Pure add/edit/delete list operations, kept separate for tests. */
internal object TemplateListLogic {
    /** Part B: at most this many message variants per moment (role). */
    const val MAX_VARIANTS_PER_ROLE = 5

    fun upsert(templates: List<MessageTemplate>, template: MessageTemplate): List<MessageTemplate> =
        if (templates.any { it.id == template.id }) {
            templates.map { if (it.id == template.id) template else it }
        } else {
            templates + template
        }

    /** Deletes by id, but never removes the last remaining template. */
    fun delete(templates: List<MessageTemplate>, id: String): List<MessageTemplate> =
        if (templates.size <= 1) templates else templates.filterNot { it.id == id }

    /**
     * Deletes by id but never removes the last remaining variant *of that role* — each moment must
     * always keep at least one message. Other roles are untouched.
     */
    fun deleteWithinRole(templates: List<MessageTemplate>, id: String): List<MessageTemplate> {
        val target = templates.firstOrNull { it.id == id } ?: return templates
        val sameRoleCount = templates.count { it.role == target.role }
        if (sameRoleCount <= 1) return templates
        return templates.filterNot { it.id == id }
    }

    /** How many variants a given role currently holds. */
    fun countForRole(templates: List<MessageTemplate>, role: TemplateRole): Int =
        templates.count { it.role == role }

    /** Whether another variant may be added for [role] (Part B max-5 cap). */
    fun canAddForRole(templates: List<MessageTemplate>, role: TemplateRole): Boolean =
        countForRole(templates, role) < MAX_VARIANTS_PER_ROLE

    /**
     * Appends any [defaults] whose role has no variant yet in [templates]. Used to seed a role
     * added after an install was first stored, without disturbing existing variants.
     */
    fun ensureRoleDefaults(
        templates: List<MessageTemplate>,
        defaults: List<MessageTemplate>
    ): List<MessageTemplate> {
        val presentRoles = templates.map { it.role }.toSet()
        val missing = defaults.filter { it.role !in presentRoles }
        return if (missing.isEmpty()) templates else templates + missing
    }
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
                template.websiteLink,
                template.role.name
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
            websiteLink = fields.getOrNull(4)?.let(::decodeValue).orEmpty(),
            // Older lines have no role field → default to CALL_ENDED (existing behavior).
            role = fields.getOrNull(5)?.let(::decodeValue)
                ?.let { name -> runCatching { TemplateRole.valueOf(name) }.getOrNull() }
                ?: TemplateRole.CALL_ENDED
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
