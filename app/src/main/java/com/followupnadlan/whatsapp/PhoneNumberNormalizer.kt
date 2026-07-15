package com.followupnadlan.whatsapp

object PhoneNumberNormalizer {
    fun normalizeForWhatsApp(input: String): String? {
        val compact = input
            .trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        if (compact.isBlank()) return null

        val withoutPrefix = when {
            compact.startsWith("+") -> compact.drop(1)
            compact.startsWith("00") -> compact.drop(2)
            else -> compact
        }

        if (!withoutPrefix.all { it.isDigit() }) return null

        val normalized = when {
            withoutPrefix.startsWith("0") && withoutPrefix.length >= 9 ->
                "972" + withoutPrefix.drop(1)
            withoutPrefix.startsWith("972") -> withoutPrefix
            withoutPrefix.length in 8..15 -> withoutPrefix
            else -> return null
        }

        return normalized.takeIf { it.length in 8..15 }
    }

    /**
     * Display-only local Israeli format: "+972501234567"/"972501234567" -> "0501234567".
     * Non-Israeli input is returned cleaned (separators and +/00 stripped) but otherwise as-is.
     * Never use this for opening WhatsApp — wa.me needs the 972 prefix (see normalizeForWhatsApp).
     */
    fun toLocalIsraeliDisplay(input: String): String {
        val compact = input
            .trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        val withoutPrefix = when {
            compact.startsWith("+") -> compact.drop(1)
            compact.startsWith("00") -> compact.drop(2)
            else -> compact
        }

        return when {
            withoutPrefix.startsWith("972") -> "0" + withoutPrefix.drop(3)
            else -> withoutPrefix
        }
    }
}
