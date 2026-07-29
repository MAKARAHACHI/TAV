package com.followupnadlan.accessibility

/**
 * A single moment's choice for ONE frequency brake, under the "override-on-default" model that the
 * recipient-scope feature already uses (see [EffectiveRecipientScope] / [RecipientScopeCodec]).
 *
 * Deliberately THREE-way, because "follow the general default" and "no brake at all" are different
 * intents and must not collapse:
 *  - [Inherit] — "לפי הכללי": follow the general default set in הגדרות חכמות. An untouched moment is
 *    Inherit, so a fresh install keeps today's effective behavior until a moment is deliberately
 *    overridden.
 *  - [Off] — "בלי המתנה": this moment removes the brake entirely, regardless of the general default.
 *  - [Value] — this moment pins its own interval (milliseconds), regardless of the general default.
 *
 * Pure data, no Android — see EffectiveCooldownTest / CooldownChoiceCodecTest.
 */
sealed interface CooldownChoice {
    /** Follow the general default (the moment has no override stored). */
    object Inherit : CooldownChoice

    /** Override to "no brake" — distinct from [Inherit], which would inherit whatever general is. */
    object Off : CooldownChoice

    /** Override to a concrete interval in milliseconds (always positive). */
    data class Value(val millis: Long) : CooldownChoice
}

/**
 * Resolves a moment's [CooldownChoice] against the general default into the concrete window the
 * (unchanged) deciders consume. A resolved `null` means "no brake".
 *
 *  - [CooldownChoice.Inherit] ⇒ the general value (which may itself be `null` = general is off).
 *  - [CooldownChoice.Off] ⇒ `null` (no brake), independent of general.
 *  - [CooldownChoice.Value] ⇒ that value.
 *
 * This is the single place the override-on-default model is decided for a brake; the send/decision
 * path resolves here with the CURRENT moment's override + the general value and passes the result to
 * the untouched deciders. Pure logic, no Android.
 */
object EffectiveCooldown {
    fun resolve(override: CooldownChoice, generalMillis: Long?): Long? = when (override) {
        CooldownChoice.Inherit -> generalMillis
        CooldownChoice.Off -> null
        is CooldownChoice.Value -> override.millis
    }
}

/**
 * Pure codec for persisting a per-moment [CooldownChoice] as a single stored long, modeled on
 * [FollowUpCooldownSettings.readOption]/`writeOption` (OFF sentinel + UNSET fallback) and
 * [RecipientScopeCodec] (absent ⇒ inherit).
 *
 * The three states map to distinct stored longs so INHERIT and OFF never collapse:
 *  - absent / [UNSET] ⇒ [CooldownChoice.Inherit] (follow general).
 *  - [OFF_SENTINEL] ⇒ [CooldownChoice.Off] (no brake).
 *  - a positive long ⇒ [CooldownChoice.Value].
 *
 * A stored non-positive value other than the OFF sentinel degrades to Inherit rather than silently
 * turning a brake off. Pure (no Android) so the null/sentinel handling is asserted without a Context;
 * the settings store below delegates its persistence to it.
 */
object CooldownChoiceCodec {
    /** Sentinel for "the moment stores no override" — decodes to [CooldownChoice.Inherit]. */
    const val UNSET: Long = 0L

    /** Sentinel for "this moment removes the brake" — distinct from [UNSET]. */
    const val OFF_SENTINEL: Long = -1L

    /** [CooldownChoice] ⇒ the long to store. Inherit ⇒ [UNSET] so the caller can drop the key. */
    fun encode(choice: CooldownChoice): Long = when (choice) {
        CooldownChoice.Inherit -> UNSET
        CooldownChoice.Off -> OFF_SENTINEL
        is CooldownChoice.Value -> choice.millis.takeIf { it > 0L } ?: UNSET
    }

    /** A stored long ⇒ its [CooldownChoice]. Absent/unset/garbage ⇒ Inherit; the sentinel ⇒ Off. */
    fun decode(stored: Long): CooldownChoice = when {
        stored == OFF_SENTINEL -> CooldownChoice.Off
        stored > 0L -> CooldownChoice.Value(stored)
        else -> CooldownChoice.Inherit
    }
}
