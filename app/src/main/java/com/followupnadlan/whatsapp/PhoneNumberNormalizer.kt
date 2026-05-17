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
}
