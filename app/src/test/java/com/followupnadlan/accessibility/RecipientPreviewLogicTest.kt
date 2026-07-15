package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipientPreviewLogicTest {

    @Test
    fun emptyListYieldsZeroPreview() {
        val preview = RecipientPreviewLogic.summary(emptyList())
        assertEquals(0, preview.count)
        assertEquals(emptyList<String>(), preview.names)
        assertEquals(0, preview.moreCount)
    }

    @Test
    fun fewerThanMaxShowsAllNoRemainder() {
        val preview = RecipientPreviewLogic.summary(listOf("אמא", "דוד"))
        assertEquals(2, preview.count)
        assertEquals(listOf("אמא", "דוד"), preview.names)
        assertEquals(0, preview.moreCount)
    }

    @Test
    fun truncatesToMaxAndCountsRemainder() {
        val preview = RecipientPreviewLogic.summary(
            labels = listOf("אמא", "דוד", "ישראל", "רון", "נועה"),
            max = 3
        )
        assertEquals(5, preview.count)
        assertEquals(listOf("אמא", "דוד", "ישראל"), preview.names)
        assertEquals(2, preview.moreCount)
    }

    @Test
    fun blankLabelsAreDropped() {
        val preview = RecipientPreviewLogic.summary(listOf("אמא", "  ", "", "דוד"))
        assertEquals(2, preview.count)
        assertEquals(listOf("אמא", "דוד"), preview.names)
        assertEquals(0, preview.moreCount)
    }
}
