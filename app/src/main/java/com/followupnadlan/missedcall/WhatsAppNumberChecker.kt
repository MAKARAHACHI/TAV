package com.followupnadlan.missedcall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract

/**
 * Whether a SAVED contact's phone number has WhatsApp — checked BEFORE opening WhatsApp so the
 * manual open path never silently claims a chat exists for a number that provably does not (§2).
 *
 * What is possible and what is not (verified): when a SAVED contact also has WhatsApp, WhatsApp
 * writes a contacts data row with mimetype [MIMETYPE_WHATSAPP] (or [MIMETYPE_WHATSAPP_BUSINESS]).
 * We can query [ContactsContract.Data] for that row. An UNSAVED number has NO such row to check —
 * that is a real WhatsApp/Android limitation, not something this class can work around, so it must
 * report [WhatsAppNumberStatus.UNKNOWN] rather than guess.
 */
class WhatsAppNumberChecker(private val context: Context) {

    fun hasContactsPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /**
     * Never guesses NO_WHATSAPP_SAVED without proof: UNKNOWN when permission is missing, the number
     * isn't a saved contact, or any query error occurs. Only a saved contact with NO whatsapp
     * profile row confirms NO_WHATSAPP_SAVED.
     */
    fun status(phone: String): WhatsAppNumberStatus {
        if (phone.isBlank() || !hasContactsPermission()) {
            return WhatsAppNumberCheckLogic.map(
                permissionGranted = hasContactsPermission(),
                isSaved = false,
                hasWhatsAppProfileRow = false
            )
        }

        val contactId = try {
            resolveContactId(phone)
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        } ?: return WhatsAppNumberCheckLogic.map(
            permissionGranted = true,
            isSaved = false,
            hasWhatsAppProfileRow = false
        )

        val hasRow = try {
            hasWhatsAppProfileRow(contactId)
        } catch (_: SecurityException) {
            return WhatsAppNumberStatus.UNKNOWN
        } catch (_: RuntimeException) {
            return WhatsAppNumberStatus.UNKNOWN
        }

        return WhatsAppNumberCheckLogic.map(
            permissionGranted = true,
            isSaved = true,
            hasWhatsAppProfileRow = hasRow
        )
    }

    private fun resolveContactId(phone: String): String? {
        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone)
        )
        return context.contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup.CONTACT_ID),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getString(0)
        }
    }

    private fun hasWhatsAppProfileRow(contactId: String): Boolean {
        val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} IN (?, ?)"
        val args = arrayOf(contactId, MIMETYPE_WHATSAPP, MIMETYPE_WHATSAPP_BUSINESS)
        return context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            selection,
            args,
            null
        )?.use { cursor -> cursor.moveToFirst() } ?: false
    }

    private companion object {
        const val MIMETYPE_WHATSAPP = "vnd.android.cursor.item/vnd.com.whatsapp.profile"
        const val MIMETYPE_WHATSAPP_BUSINESS = "vnd.android.cursor.item/vnd.com.whatsapp.w4b.profile"
    }
}

enum class WhatsAppNumberStatus {
    HAS_WHATSAPP,
    NO_WHATSAPP_SAVED,
    UNKNOWN
}

/**
 * Pure mapping (permissionGranted, isSaved, hasWhatsAppProfileRow) -> [WhatsAppNumberStatus], kept
 * free of Android so it's testable without Robolectric (same seam pattern as
 * [com.followupnadlan.postcall.ContactNameResolverLogic]).
 *
 * §2: NO_WHATSAPP_SAVED is returned ONLY when the number is a saved, verifiable contact confirmed to
 * have no WhatsApp profile row. Every other case — no permission, not saved, any doubt — is UNKNOWN;
 * we never assert the absence of WhatsApp for a number we can't actually verify.
 */
object WhatsAppNumberCheckLogic {
    fun map(permissionGranted: Boolean, isSaved: Boolean, hasWhatsAppProfileRow: Boolean): WhatsAppNumberStatus =
        when {
            !permissionGranted -> WhatsAppNumberStatus.UNKNOWN
            !isSaved -> WhatsAppNumberStatus.UNKNOWN
            hasWhatsAppProfileRow -> WhatsAppNumberStatus.HAS_WHATSAPP
            else -> WhatsAppNumberStatus.NO_WHATSAPP_SAVED
        }

    /**
     * The manual-open-path branch decision ([MissedCallAutoResponseHandler.openPreparedWhatsApp]):
     * only a CONFIRMED [WhatsAppNumberStatus.NO_WHATSAPP_SAVED] takes the honest fallback instead of
     * opening WhatsApp. UNKNOWN and HAS_WHATSAPP both open exactly as before — we only ever change
     * behavior on a confirmed absence, never on doubt.
     */
    fun shouldTakeNotOnWhatsAppFallback(status: WhatsAppNumberStatus): Boolean =
        status == WhatsAppNumberStatus.NO_WHATSAPP_SAVED
}
