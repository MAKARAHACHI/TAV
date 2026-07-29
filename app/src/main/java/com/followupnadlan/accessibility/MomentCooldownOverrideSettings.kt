package com.followupnadlan.accessibility

import android.content.Context

/**
 * A single moment's per-moment overrides for the two frequency brakes, under the "override-on-default"
 * model (mirrors [EndedScopeSettings]/[NoAnswerScopeSettings] for recipient scope).
 *
 * ONE class, reused per moment via a moment-scoped preferences name ([forMoment]) — not six
 * near-identical classes. The general default stays in [FollowUpCooldownSettings]; this store only
 * records whether a moment follows it ([CooldownChoice.Inherit]), removes the brake
 * ([CooldownChoice.Off]), or pins its own interval ([CooldownChoice.Value]).
 *
 * Storage only. The effective window is resolved at the call site via [EffectiveCooldown.resolve]
 * with the general value; the deciders are unchanged. An untouched moment stores nothing, so it reads
 * as Inherit and keeps today's effective behavior.
 */
class MomentCooldownOverrideSettings private constructor(context: Context, preferencesName: String) {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    /** "כל כמה זמן לשלוח שוב לאותו אדם" for THIS moment. Inherit = follow the general default. */
    var sameNumberOverride: CooldownChoice
        get() = read(KEY_SAME_NUMBER)
        set(value) = write(KEY_SAME_NUMBER, value)

    /** "מרווח מינימלי בין הודעות" for THIS moment. Inherit = follow the general default. */
    var globalQuietOverride: CooldownChoice
        get() = read(KEY_GLOBAL_QUIET)
        set(value) = write(KEY_GLOBAL_QUIET, value)

    private fun read(key: String): CooldownChoice =
        CooldownChoiceCodec.decode(preferences.getLong(key, CooldownChoiceCodec.UNSET))

    private fun write(key: String, choice: CooldownChoice) {
        val encoded = CooldownChoiceCodec.encode(choice)
        preferences.edit().apply {
            // Inherit encodes to UNSET; drop the key so an untouched moment truly has no stored value.
            if (encoded == CooldownChoiceCodec.UNSET) remove(key) else putLong(key, encoded)
        }.apply()
    }

    companion object {
        private const val KEY_SAME_NUMBER = "same_number_override"
        private const val KEY_GLOBAL_QUIET = "global_quiet_override"

        /** The per-moment preferences file names. Each moment gets its own, so they stay independent. */
        private const val PREFS_MISSED = "cooldown_override_missed"
        private const val PREFS_ENDED = "cooldown_override_ended"
        private const val PREFS_NO_ANSWER = "cooldown_override_no_answer"

        internal fun forMoment(context: Context, kind: MomentEditKind): MomentCooldownOverrideSettings {
            val name = when (kind) {
                MomentEditKind.MISSED -> PREFS_MISSED
                MomentEditKind.ENDED -> PREFS_ENDED
                MomentEditKind.NO_ANSWER -> PREFS_NO_ANSWER
            }
            return MomentCooldownOverrideSettings(context, name)
        }

        // Engine-side factories, so decision code (postcall/ missedcall/) needn't reference the UI's
        // MomentEditKind — the SAME three preference files, one class.
        fun forMissed(context: Context) = MomentCooldownOverrideSettings(context, PREFS_MISSED)
        fun forEnded(context: Context) = MomentCooldownOverrideSettings(context, PREFS_ENDED)
        fun forNoAnswer(context: Context) = MomentCooldownOverrideSettings(context, PREFS_NO_ANSWER)
    }
}
