package com.followupnadlan.accessibility

/**
 * Resolves the "override-on-default" model for a moment's recipient scope.
 *
 * Each moment (missed / ended / no-answer) either pins its own [RecipientScope] (a per-moment
 * override) or leaves the choice on "כמו הכללי" — follow the general default set in Smart-Rules.
 * "כמו הכללי" is represented as a `null` per-moment value; a concrete [RecipientScope] is an
 * override.
 *
 * Pure logic, no Android — see EffectiveRecipientScopeTest.
 */
object EffectiveRecipientScope {
    /**
     * The scope actually fed to a decider: the per-moment override when set, otherwise the general
     * default. This is the single place "override-on-default" is decided; call sites resolve here
     * and pass the result to the (untouched) deciders.
     */
    fun of(perMoment: RecipientScope?, general: RecipientScope): RecipientScope = perMoment ?: general
}

/**
 * Pure codec for a nullable per-moment scope: `null` ("כמו הכללי" / follow general) round-trips to
 * an absent stored value, and a concrete [RecipientScope] round-trips to/from its enum name. Kept
 * pure (no Android) so the null-handling can be asserted without a Context — the settings stores
 * below delegate their persistence to it.
 *
 * See RecipientScopeCodecTest.
 */
object RecipientScopeCodec {
    /** null ⇒ null (store nothing = follow general); a scope ⇒ its stable enum name. */
    fun encode(scope: RecipientScope?): String? = scope?.name

    /** A stored name ⇒ its scope; null or an unrecognized value ⇒ null (follow general). */
    fun decode(stored: String?): RecipientScope? =
        stored?.let { runCatching { RecipientScope.valueOf(it) }.getOrNull() }
}
