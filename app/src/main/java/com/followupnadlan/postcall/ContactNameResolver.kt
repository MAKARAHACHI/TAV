package com.followupnadlan.postcall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract

class ContactNameResolver(private val context: Context) {
    fun resolveFirstName(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        return try {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                lookupUri,
                CONTACT_PROJECTION,
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                ContactNameResolverLogic.extractFirstName(cursor.getString(COLUMN_DISPLAY_NAME))
            }
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private companion object {
        val CONTACT_PROJECTION = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        const val COLUMN_DISPLAY_NAME = 0
    }
}

object ContactNameResolverLogic {
    fun extractFirstName(displayName: String?): String? {
        return displayName
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
