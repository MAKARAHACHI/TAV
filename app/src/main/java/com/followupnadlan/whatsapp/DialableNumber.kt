package com.followupnadlan.whatsapp

/**
 * Whether a number is one a real person can actually receive a message on.
 *
 * §2 rationale: wa.me simply does not resolve for service codes, toll-free numbers or withheld
 * callers, and an SMS to them is equally pointless. Offering to follow up with such a "caller"
 * promises the user an outcome that cannot happen — so these are filtered out locally, before
 * anything is offered or sent, rather than failing later and looking like a bug.
 *
 * Pure logic, no Android — see DialableNumberTest.
 */
object DialableNumber {
    /** Below this, it is a short code or a service number, not a subscriber line. */
    private const val MIN_SUBSCRIBER_DIGITS = 7

    private val PRIVATE_OR_UNKNOWN_LABELS =
        setOf("unknown", "private", "anonymous", "restricted", "unavailable", "withheld", "blocked")

    private val EMERGENCY_OR_SERVICE_NUMBERS =
        setOf("100", "101", "102", "103", "104", "105", "106", "110", "112", "911", "999", "000")

    /**
     * Toll-free / premium prefixes (IL 1-800 & 1-700, NANP 800/888/877/866/855/844/833).
     * These are business lines: a client is never actually reachable there.
     */
    private val TOLL_FREE_PREFIXES =
        setOf("1800", "1700", "1900", "800", "888", "877", "866", "855", "844", "833")

    fun isDialable(phone: String?): Boolean {
        val trimmed = phone.orEmpty().trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.lowercase() in PRIVATE_OR_UNKNOWN_LABELS) return false

        // A caller id containing * or # is a network/service code (e.g. *6555), never a subscriber.
        if (trimmed.any { it == '*' || it == '#' }) return false

        val digits = trimmed.filter { it.isDigit() }
        if (digits.length < MIN_SUBSCRIBER_DIGITS) return false
        if (digits in EMERGENCY_OR_SERVICE_NUMBERS) return false
        if (isTollFree(digits)) return false

        return true
    }

    /**
     * Checks the number both as dialed and with an Israeli country code stripped, so that
     * "1800123456" and "9721800123456" are both recognised as the same toll-free line.
     */
    private fun isTollFree(digits: String): Boolean {
        val candidates = listOf(digits, digits.removePrefix("972"))
        return candidates.any { candidate ->
            TOLL_FREE_PREFIXES.any(candidate::startsWith)
        }
    }
}
