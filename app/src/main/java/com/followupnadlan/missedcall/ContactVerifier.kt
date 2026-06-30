package com.followupnadlan.missedcall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract

/**
 * Verifies whether an incoming number belongs to a saved contact. Used by the
 * "contacts only" recipient mode: the app only messages saved contacts, and it must
 * never silently treat an unverifiable number as allowed.
 */
class ContactVerifier(private val context: Context) {

    fun hasContactsPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /**
     * Returns true only if [phoneNumber] resolves to a saved contact. Returns false when
     * the number is blank, permission is missing, or the lookup fails — verification must
     * fail closed so contacts-only never sends to an unknown number.
     */
    fun isSavedContact(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        if (!hasContactsPermission()) return false

        return try {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { cursor -> cursor.moveToFirst() } ?: false
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }
}
