package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateMigrationLogicTest {
    private val defaults = SprintOneTemplates.all

    @Test
    fun seedsDefaultsWhenNoOldBodies() {
        val migrated = TemplateMigrationLogic.migrate(
            defaults = defaults,
            oldBodiesById = defaults.associate { it.id to null }
        )
        assertEquals(defaults, migrated)
    }

    @Test
    fun keepsUserCustomizedOldBody() {
        val custom = "הנוסח הערוך שלי"
        val migrated = TemplateMigrationLogic.migrate(
            defaults = defaults,
            oldBodiesById = mapOf(SprintOneTemplates.ENDED_ID to custom)
        )
        assertEquals(custom, migrated.first { it.id == SprintOneTemplates.ENDED_ID }.body)
    }

    @Test
    fun dropsLegacyDefaultBodyInFavorOfNewCopy() {
        val legacyBody = "שלום, שמחתי לדבר איתך לגבי הדירה. מצרף פרטים."
        val migrated = TemplateMigrationLogic.migrate(
            defaults = defaults,
            oldBodiesById = mapOf(SprintOneTemplates.ENDED_ID to legacyBody)
        )
        val gentleDefault = defaults.first { it.id == SprintOneTemplates.ENDED_ID }.body
        assertEquals(gentleDefault, migrated.first { it.id == SprintOneTemplates.ENDED_ID }.body)
    }

    @Test
    fun migratedTemplatesHaveEmptyLinks() {
        val migrated = TemplateMigrationLogic.migrate(
            defaults = defaults,
            oldBodiesById = mapOf(SprintOneTemplates.ENDED_ID to "custom")
        )
        migrated.forEach {
            assertEquals("", it.cardLink)
            assertEquals("", it.websiteLink)
        }
    }
}
