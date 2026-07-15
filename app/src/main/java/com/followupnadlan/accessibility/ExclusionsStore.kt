package com.followupnadlan.accessibility

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * A person or number the app will not message automatically.
 * [label] is what the user sees (a contact name, or the number itself).
 * [number] is the phone number when known; blank for a name-only entry.
 */
data class ExcludedRecipient(
    val label: String,
    val number: String = ""
)

enum class BlockedRecipientGroup {
    CONTACTS,
    NON_CONTACTS,
    FIRST_TIME
}

/**
 * Local list of recipients to skip. Storage only: it persists the user's choices.
 * No new sending or detection logic — screens read/write this list, nothing else.
 */
class ExclusionsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<ExcludedRecipient> =
        ExclusionsCodec.decode(preferences.getString(KEY_ENTRIES, "").orEmpty())

    fun save(entries: List<ExcludedRecipient>) {
        preferences.edit()
            .putString(KEY_ENTRIES, ExclusionsCodec.encode(entries))
            .commit()
    }

    fun add(entry: ExcludedRecipient) {
        val current = load()
        if (current.any { it.matchesEntry(entry) }) return
        save(current + entry)
    }

    fun remove(entry: ExcludedRecipient) {
        save(load().filterNot { it.matchesEntry(entry) })
    }

    fun loadBlockedGroups(): Set<BlockedRecipientGroup> {
        val raw = preferences.getStringSet(KEY_BLOCKED_GROUPS, emptySet()).orEmpty()
        return raw.mapNotNull { value ->
            runCatching { BlockedRecipientGroup.valueOf(value) }.getOrNull()
        }.toSet()
    }

    fun setBlockedGroup(group: BlockedRecipientGroup, blocked: Boolean) {
        val next = loadBlockedGroups().toMutableSet()
        if (blocked) {
            next.add(group)
        } else {
            next.remove(group)
        }
        preferences.edit()
            .putStringSet(KEY_BLOCKED_GROUPS, next.map { it.name }.toSet())
            .commit()
    }

    /** True if [phoneNumber] (raw or normalized) matches any excluded number entry. */
    fun isExcluded(phoneNumber: String?): Boolean = ExclusionMatcher.isExcluded(load(), phoneNumber)

    private companion object {
        const val PREFERENCES_NAME = "missed_call_exclusions"
        const val KEY_ENTRIES = "entries"
        const val KEY_BLOCKED_GROUPS = "blocked_groups"
    }
}

private fun ExcludedRecipient.matchesEntry(other: ExcludedRecipient): Boolean =
    number.isNotBlank() && other.number.isNotBlank() && number.filter { it.isDigit() }.takeLast(7) == other.number.filter { it.isDigit() }.takeLast(7) ||
        label == other.label && number == other.number

/**
 * Pure number-matching for exclusions. Compares by trailing digits so that the same
 * person matches whether stored/dialed as local (050…) or international (+97250…).
 */
object ExclusionMatcher {
    private const val MATCH_DIGITS = 7

    fun isExcluded(entries: List<ExcludedRecipient>, phoneNumber: String?): Boolean {
        val target = significantDigits(phoneNumber) ?: return false
        return entries.any { entry ->
            val entryDigits = significantDigits(entry.number) ?: return@any false
            entryDigits == target
        }
    }

    private fun significantDigits(value: String?): String? {
        val digits = value.orEmpty().filter { it.isDigit() }
        if (digits.length < MATCH_DIGITS) return null
        return digits.takeLast(MATCH_DIGITS)
    }
}

internal object ExclusionsCodec {
    private const val FIELD_SEPARATOR = "|"

    fun encode(entries: List<ExcludedRecipient>): String =
        entries.joinToString(separator = "\n") { entry ->
            listOf(encodeValue(entry.label), encodeValue(entry.number)).joinToString(FIELD_SEPARATOR)
        }

    fun decode(raw: String): List<ExcludedRecipient> =
        raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull(::decodeLine)
            .toList()

    private fun decodeLine(line: String): ExcludedRecipient? {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.isEmpty()) return null
        val label = decodeValue(fields[0])
        val number = fields.getOrNull(1)?.let(::decodeValue).orEmpty()
        if (label.isBlank() && number.isBlank()) return null
        return ExcludedRecipient(label = label.ifBlank { number }, number = number)
    }

    private fun encodeValue(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeValue(value: String): String =
        runCatching { String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8) }.getOrDefault("")
}
