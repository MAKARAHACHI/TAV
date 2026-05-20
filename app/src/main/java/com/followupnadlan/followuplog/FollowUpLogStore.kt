package com.followupnadlan.followuplog

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64

class FollowUpLogStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<FollowUpLogEntry> = FollowUpLogStorage.decode(preferences.getString(KEY_ENTRIES, "").orEmpty())

    fun append(entry: FollowUpLogEntry) {
        preferences.edit()
            .putString(KEY_ENTRIES, FollowUpLogStorage.append(preferences.getString(KEY_ENTRIES, "").orEmpty(), entry))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "follow_up_action_log"
        const val KEY_ENTRIES = "entries"
    }
}

internal object FollowUpLogStorage {
    const val MAX_ENTRIES = 50
    const val MESSAGE_PREVIEW_MAX_CHARS = 120

    fun append(
        rawEntries: String,
        entry: FollowUpLogEntry,
        maxEntries: Int = MAX_ENTRIES
    ): String = encode((decode(rawEntries) + entry).takeLast(maxEntries))

    fun decode(rawEntries: String): List<FollowUpLogEntry> =
        rawEntries.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull(::decodeLine)
            .toList()

    fun encode(entries: List<FollowUpLogEntry>): String = entries.joinToString(separator = "\n", transform = ::encodeLine)

    fun messagePreview(message: String, maxChars: Int = MESSAGE_PREVIEW_MAX_CHARS): String {
        val compact = message.trim().replace(Regex("\\s+"), " ")
        return if (compact.length <= maxChars) compact else compact.take(maxChars).trimEnd()
    }

    private fun encodeLine(entry: FollowUpLogEntry): String = listOf(
        VERSION,
        encodeValue(entry.leadName),
        encodeValue(entry.phoneNumber),
        encodeValue(entry.templateId),
        encodeValue(entry.templateTitle),
        encodeValue(entry.propertyName),
        encodeValue(entry.propertyLink),
        encodeValue(entry.messagePreview),
        entry.actionType.name,
        entry.timestampEpochMs.toString()
    ).joinToString(FIELD_SEPARATOR)

    private fun decodeLine(line: String): FollowUpLogEntry? {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT || fields[0] != VERSION) return null

        val actionType = runCatching { FollowUpActionType.valueOf(fields[8]) }.getOrNull() ?: return null
        val timestamp = fields[9].toLongOrNull() ?: return null

        return FollowUpLogEntry(
            leadName = decodeValue(fields[1]),
            phoneNumber = decodeValue(fields[2]),
            templateId = decodeValue(fields[3]),
            templateTitle = decodeValue(fields[4]),
            propertyName = decodeValue(fields[5]),
            propertyLink = decodeValue(fields[6]),
            messagePreview = decodeValue(fields[7]),
            actionType = actionType,
            timestampEpochMs = timestamp
        )
    }

    private fun encodeValue(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeValue(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private const val VERSION = "v1"
    private const val FIELD_SEPARATOR = "|"
    private const val FIELD_COUNT = 10
}
