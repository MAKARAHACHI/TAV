package com.followupnadlan.accessibility

import android.content.Context
import androidx.compose.ui.text.input.KeyboardType
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import java.nio.charset.StandardCharsets
import java.util.Base64

data class AllowedRecipient(
    val label: String,
    val number: String
)

class AllowedRecipientsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<AllowedRecipient> =
        AllowedRecipientsCodec.decode(preferences.getString(KEY_ENTRIES, "").orEmpty())

    fun save(entries: List<AllowedRecipient>) {
        preferences.edit()
            .putString(KEY_ENTRIES, AllowedRecipientsCodec.encode(entries))
            .commit()
    }

    fun add(entry: AllowedRecipient) {
        val validEntry = AllowedRecipientValidator.validEntry(entry) ?: return
        val current = load()
        if (current.any { AllowedRecipientMatcher.sameNumber(it.number, validEntry.number) }) return
        save(current + validEntry)
    }

    fun remove(entry: AllowedRecipient) {
        save(load().filterNot { AllowedRecipientMatcher.sameNumber(it.number, entry.number) })
    }

    fun isAllowed(phoneNumber: String?): Boolean =
        AllowedRecipientMatcher.isAllowed(load(), phoneNumber)

    private companion object {
        const val PREFERENCES_NAME = "missed_call_allowed_recipients"
        const val KEY_ENTRIES = "entries"
    }
}

object AllowedRecipientValidator {
    fun validEntry(entry: AllowedRecipient): AllowedRecipient? {
        val normalized = PhoneNumberNormalizer.normalizeForWhatsApp(entry.number) ?: return null
        val label = entry.label.trim().ifBlank { entry.number.trim() }
        return AllowedRecipient(label = label, number = normalized)
    }
}

object AllowedRecipientMatcher {
    fun isAllowed(entries: List<AllowedRecipient>, phoneNumber: String?): Boolean {
        val normalized = PhoneNumberNormalizer.normalizeForWhatsApp(phoneNumber.orEmpty()) ?: return false
        return entries.any { sameNumber(it.number, normalized) }
    }

    fun sameNumber(left: String, right: String): Boolean {
        val normalizedLeft = PhoneNumberNormalizer.normalizeForWhatsApp(left) ?: return false
        val normalizedRight = PhoneNumberNormalizer.normalizeForWhatsApp(right) ?: return false
        return normalizedLeft == normalizedRight
    }
}

object AllowedRecipientsUiSpec {
    const val TITLE = "רק למי לשלוח?"
    const val DESCRIPTION = "בחר/י מספרים שהאפליקציה כן תשלח להם הודעה אוטומטית."
    const val EMPTY_STATE = "עדיין לא בחרת מספרים. במצב הזה לא תישלח הודעה אוטומטית לאף אחד."
    const val ITEM_SUBTITLE = "תישלח הודעה אוטומטית אם לא תענה/י"
    const val INVALID_NUMBER = "בדוק/י שהמספר תקין"
    val phoneKeyboardType: KeyboardType = KeyboardType.Phone
}

internal object AllowedRecipientsCodec {
    private const val FIELD_SEPARATOR = "|"

    fun encode(entries: List<AllowedRecipient>): String =
        entries.joinToString(separator = "\n") { entry ->
            listOf(encodeValue(entry.label), encodeValue(entry.number)).joinToString(FIELD_SEPARATOR)
        }

    fun decode(raw: String): List<AllowedRecipient> =
        raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull(::decodeLine)
            .toList()

    private fun decodeLine(line: String): AllowedRecipient? {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size < 2) return null
        val label = decodeValue(fields[0])
        val number = decodeValue(fields[1])
        if (number.isBlank()) return null
        return AllowedRecipient(label = label.ifBlank { number }, number = number)
    }

    private fun encodeValue(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeValue(value: String): String =
        runCatching { String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8) }.getOrDefault("")
}
