package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ActivityFeedTest {
    private val utc = ZoneId.of("UTC")

    private fun entry(type: FollowUpActionType, phone: String = "0501234567") = FollowUpLogEntry(
        actionType = type,
        timestampEpochMs = 1_700_000_000_000L,
        messagePreview = "raw internal preview text",
        phone = phone,
        source = "missed_call_auto_response"
    )

    @Test
    fun excludedNumberShowsChosenNotToSendRow() {
        val rows = ActivityFeed.rows(listOf(entry(FollowUpActionType.AUTO_SMS_SKIPPED_EXCLUDED)), utc)
        assertEquals(1, rows.size)
        assertEquals("לא נשלח — בחרת לא לשלוח למספר הזה", rows.first().title)
    }

    @Test
    fun contactsOnlyShowsNoContactsPermissionRow() {
        val rows = ActivityFeed.rows(listOf(entry(FollowUpActionType.AUTO_SMS_SKIPPED_CONTACTS_ONLY)), utc)
        assertEquals(1, rows.size)
        assertEquals("לא נשלח — אין הרשאת אנשי קשר כדי לוודא שהמספר שמור.", rows.first().title)
    }

    @Test
    fun duplicateShowsFriendlyDuplicateRow() {
        val rows = ActivityFeed.rows(listOf(entry(FollowUpActionType.AUTO_SMS_SKIPPED_DUPLICATE)), utc)
        assertEquals("לא נשלח בגלל הגנת כפילויות", rows.first().title)
    }

    @Test
    fun friendlyRowsNeverExposeInternalLabels() {
        val allTypes = FollowUpActionType.values().map { entry(it) }
        val rows = ActivityFeed.rows(allTypes, utc)

        assertTrue(rows.isNotEmpty())
        rows.forEach { row ->
            // No enum names, raw source ids, or internal preview text in the user-facing title.
            FollowUpActionType.values().forEach { type ->
                assertFalse("title leaked enum name ${type.name}", row.title.contains(type.name))
            }
            assertFalse("title leaked source id", row.title.contains("missed_call_auto_response"))
            assertFalse("title leaked raw preview", row.title.contains("raw internal preview text"))
            assertFalse("title leaked underscores", row.title.contains("_"))
        }
    }

    @Test
    fun internalOnlyActionsAreFilteredOut() {
        // WHATSAPP_AUTO_SEND_ATTEMPTED is an internal step, not a friendly outcome.
        val rows = ActivityFeed.rows(listOf(entry(FollowUpActionType.WHATSAPP_AUTO_SEND_ATTEMPTED)), utc)
        assertTrue(rows.isEmpty())
    }
}
