package com.followupnadlan.accessibility

/** Compact summary of a saved recipient list for the Settings screen. */
data class RecipientPreview(
    val count: Int,
    val names: List<String>,
    val moreCount: Int
)

object RecipientPreviewLogic {
    /**
     * Summarizes [labels] into count + up to [max] names + a "+N more" remainder.
     * Blank labels are dropped; count reflects the non-blank labels.
     */
    fun summary(labels: List<String>, max: Int = 3): RecipientPreview {
        val cleaned = labels.map { it.trim() }.filter { it.isNotBlank() }
        val shown = cleaned.take(max)
        return RecipientPreview(
            count = cleaned.size,
            names = shown,
            moreCount = (cleaned.size - shown.size).coerceAtLeast(0)
        )
    }
}
