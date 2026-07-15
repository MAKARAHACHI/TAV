package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals("לא נשלח — אין הרשאת אנשי קשר כדי לוודא שהמספר שמור", rows.first().title)
    }

    @Test
    fun onlySelectedUnlistedNumberShowsFriendlyRow() {
        val rows = ActivityFeed.rows(listOf(entry(FollowUpActionType.AUTO_SMS_SKIPPED_NOT_ALLOWED)), utc)
        assertEquals(1, rows.size)
        assertEquals("לא נשלח — המספר לא נמצא ברשימת המותרים", rows.first().title)
    }

    @Test
    fun blockGroupsShowFriendlyRows() {
        val rows = ActivityFeed.rows(
            listOf(
                entry(FollowUpActionType.AUTO_SMS_SKIPPED_BLOCKED_CONTACT),
                entry(FollowUpActionType.AUTO_SMS_SKIPPED_BLOCKED_NON_CONTACT),
                entry(FollowUpActionType.AUTO_SMS_SKIPPED_FIRST_TIME),
                entry(FollowUpActionType.AUTO_SMS_SKIPPED_CONTACT_TYPE_UNVERIFIED)
            ),
            utc
        )

        assertTrue(rows.any { it.title == "לא נשלח — אנשי קשר חסומים בהגדרה שלך" })
        assertTrue(rows.any { it.title == "לא נשלח — מספרים לא שמורים חסומים בהגדרה שלך" })
        assertTrue(rows.any { it.title == "לא נשלח — מספר חדש בפעם הראשונה" })
        assertTrue(rows.any { it.title == "לא נשלח — אין הרשאת אנשי קשר כדי לוודא את סוג המספר." })
    }

    @Test
    fun manualPendingShowsWaitingForApprovalRow() {
        val rows = ActivityFeed.rows(listOf(entry(FollowUpActionType.MANUAL_REPLY_PENDING)), utc)
        assertEquals("ממתין לאישור שלך", rows.first().title)
    }

    @Test
    fun manualComposerOpenDoesNotClaimSent() {
        val rows = ActivityFeed.rows(
            listOf(
                entry(FollowUpActionType.MANUAL_WHATSAPP_COMPOSER_OPENED),
                entry(FollowUpActionType.MANUAL_SMS_COMPOSER_OPENED)
            ),
            utc
        )

        assertEquals(2, rows.size)
        assertTrue(rows.any { it.title == "נפתחה שיחת WhatsApp" })
        rows.forEach { row ->
            assertFalse(row.title.contains("נשלח"))
            assertFalse(row.title.contains("נשלחה"))
        }
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
        val rows = ActivityFeed.rows(listOf(entry(FollowUpActionType.WHATSAPP_AUTO_SEND_ATTEMPTED)), utc)
        assertTrue(rows.isEmpty())
    }

    @Test
    fun phoneIsShownInLocalIsraeliFormat() {
        val rows = ActivityFeed.rows(
            listOf(entry(FollowUpActionType.MISSED_CALL_DETECTED, phone = "972501234567")),
            utc
        )
        assertEquals("0501234567", rows.first().phone)
        assertNull(rows.first().contactName)
    }

    @Test
    fun contactNameIsResolvedByLastNineDigits() {
        val rows = ActivityFeed.rows(
            listOf(entry(FollowUpActionType.MISSED_CALL_DETECTED, phone = "972501234567")),
            utc
        ) { phone -> if (phone.filter(Char::isDigit).takeLast(9) == "501234567") "דנה" else null }
        assertEquals("דנה", rows.first().contactName)
        assertEquals("0501234567", rows.first().phone)
    }
}
