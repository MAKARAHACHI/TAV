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
    const val MAX_ENTRIES = 100
    const val MESSAGE_PREVIEW_MAX_CHARS = 80

    fun append(
        rawEntries: String,
        entry: FollowUpLogEntry,
        maxEntries: Int = MAX_ENTRIES
    ): String = encode((decode(rawEntries) + entry.copy(messagePreview = messagePreview(entry.messagePreview))).takeLast(maxEntries))

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
        entry.timestampEpochMs.toString(),
        entry.actionType.name,
        encodeValue(messagePreview(entry.messagePreview))
    ).joinToString(FIELD_SEPARATOR)

    private fun decodeLine(line: String): FollowUpLogEntry? {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) return null

        val timestamp = fields[0].toLongOrNull() ?: return null
        val actionType = runCatching { FollowUpActionType.valueOf(fields[1]) }.getOrNull() ?: return null

        return FollowUpLogEntry(
            actionType = actionType,
            timestampEpochMs = timestamp,
            messagePreview = messagePreview(decodeValue(fields[2]))
        )
    }

    private fun encodeValue(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeValue(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private const val FIELD_SEPARATOR = "|"
    private const val FIELD_COUNT = 3
}
