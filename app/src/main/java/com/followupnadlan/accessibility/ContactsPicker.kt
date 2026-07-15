package com.followupnadlan.accessibility

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import com.followupnadlan.whatsapp.PhoneNumberNormalizer

data class ContactCandidate(
    val name: String,
    val phone: String
)

/** Shared recipient key: last 9 digits of the phone (matches ContactsPickerRepository dedup). */
fun recipientKey(phone: String): String = phone.filter(Char::isDigit).takeLast(9)

class ContactsPickerRepository(private val context: Context) {
    fun load(): List<ContactCandidate> {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        return try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                PROJECTION,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY
            )?.use { cursor ->
                buildList {
                    val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val phoneIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        val phone = cursor.getString(phoneIndex).orEmpty().trim()
                        if (phone.isNotBlank()) {
                            add(
                                ContactCandidate(
                                    name = cursor.getString(nameIndex).orEmpty().trim().ifBlank { phone },
                                    phone = phone
                                )
                            )
                        }
                    }
                }.distinctBy { recipientKey(it.phone) }
            }.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    private companion object {
        val PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
    }
}

object ContactsPickerSearch {
    fun filter(contacts: List<ContactCandidate>, query: String): List<ContactCandidate> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return contacts

        val queryDigits = trimmed.filter(Char::isDigit)
        val queryText = trimmed.lowercase()
        return contacts.filter { contact ->
            contact.name.lowercase().contains(queryText) ||
                contact.phone.lowercase().contains(queryText) ||
                (queryDigits.isNotBlank() && contact.phone.filter(Char::isDigit).contains(queryDigits))
        }
    }
}

object ContactsPickerPermissionState {
    const val DENIED_MESSAGE = "כדי לבחור מאנשי קשר צריך לאשר גישה. אפשר עדיין להוסיף מספר ידנית."

    fun deniedMessage(permissionDenied: Boolean): String? =
        if (permissionDenied) DENIED_MESSAGE else null
}

/**
 * Pure diff between what is already saved and what is currently checked in the multi-picker.
 * Keys are last-9-digits (see [recipientKey]).
 *
 * added   = checked − saved (new selections to persist)
 * removed = (saved ∩ contactKeys) − checked  — only un-checking a currently-listed contact removes it.
 * Intersecting with [contactKeys] protects manually-added / name-only entries that are not in the
 * current candidate list from ever being deleted by a picker confirm.
 */
object ContactSelectionDiff {
    data class Result(val added: Set<String>, val removed: Set<String>)

    fun compute(
        savedKeys: Set<String>,
        checkedKeys: Set<String>,
        contactKeys: Set<String>
    ): Result {
        val added = checkedKeys - savedKeys
        val removed = (savedKeys intersect contactKeys) - checkedKeys
        return Result(added = added, removed = removed)
    }
}

/** Raw recent-caller row from Call Log or the follow-up log, before dedup/mapping. */
data class RecentCallerRow(
    val name: String?,
    val phone: String,
    val timestampEpochMs: Long
)

/**
 * Pure mapping of raw recent-caller rows to display candidates: drops rows without a phone,
 * keeps the newest per number (distinct by last-9-digits), sorts newest-first, and caps the count.
 * name = known caller name, otherwise the number in local 05… form ("unsaved number").
 */
object RecentCallersLogic {
    const val MAX_RESULTS = 20

    fun dedupe(rows: List<RecentCallerRow>, max: Int = MAX_RESULTS): List<ContactCandidate> =
        rows.asSequence()
            .filter { it.phone.filter(Char::isDigit).isNotEmpty() }
            .sortedByDescending { it.timestampEpochMs }
            .distinctBy { recipientKey(it.phone) }
            .take(max)
            .map { row ->
                val label = row.name?.trim()?.takeIf { it.isNotBlank() }
                    ?: PhoneNumberNormalizer.toLocalIsraeliDisplay(row.phone)
                ContactCandidate(name = label, phone = row.phone)
            }
            .toList()
}
