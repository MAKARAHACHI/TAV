package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyTemplateMigrationTest {

    @Test
    fun legacySelectedIdMigratesToGentleDefault() {
        LegacyTemplateMigration.LEGACY_TEMPLATE_IDS.forEach { legacyId ->
            assertEquals(
                "legacy id $legacyId should migrate to gentle default",
                SprintOneTemplates.GENTLE_ID,
                LegacyTemplateMigration.migrateSelectedTemplateId(legacyId)
            )
        }
    }

    @Test
    fun nullSelectedIdResolvesToGentleDefault() {
        assertEquals(SprintOneTemplates.GENTLE_ID, LegacyTemplateMigration.migrateSelectedTemplateId(null))
    }

    @Test
    fun accessibilitySelectedIdIsKept() {
        assertEquals(
            SprintOneTemplates.PRIVATE_ID,
            LegacyTemplateMigration.migrateSelectedTemplateId(SprintOneTemplates.PRIVATE_ID)
        )
    }

    @Test
    fun legacyBusinessDefaultBodyIsReplaced() {
        // Old "missed_call_auto_response" default body.
        val legacyBody = """
            שלום, תודה שפניתם ל{{businessName}}.
            אנחנו כרגע בשטח או בעבודה ולכן לא תמיד יכולים לענות מיד.
            קיבלנו את פנייתכם ונחזור אליכם בהקדם.
        """.trimIndent()
        assertTrue(LegacyTemplateMigration.isLegacyDefaultBody(legacyBody))

        // Old real-estate templates too.
        assertTrue(LegacyTemplateMigration.isLegacyDefaultBody("שלום, שמחתי לדבר איתך לגבי הדירה. מצרף פרטים."))
        assertTrue(LegacyTemplateMigration.isLegacyDefaultBody("ראיתי שפספסתי את השיחה שלך."))
    }

    @Test
    fun userCustomizedAccessibilityBodyIsNotReplaced() {
        val custom = "שלום, אני מעדיף/ה תקשורת בכתב. אפשר לכתוב לי כאן ואחזור אליך."
        assertFalse(LegacyTemplateMigration.isLegacyDefaultBody(custom))
    }

    @Test
    fun blankBodyIsNotTreatedAsLegacy() {
        assertFalse(LegacyTemplateMigration.isLegacyDefaultBody(null))
        assertFalse(LegacyTemplateMigration.isLegacyDefaultBody(""))
        assertFalse(LegacyTemplateMigration.isLegacyDefaultBody("   "))
    }
}
