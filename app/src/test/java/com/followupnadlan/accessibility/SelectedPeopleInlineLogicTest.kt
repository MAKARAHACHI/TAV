package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure wording for the "רק אנשים שאבחר" inline preview on the moment edit page. Fed a
 * [RecipientPreview] (produced by [RecipientPreviewLogic.summary]), it renders the one-line summary
 * the edit page shows so the user sees who is on the list without opening ALLOWED_RECIPIENTS.
 */
class SelectedPeopleInlineLogicTest {

    @Test
    fun emptyListShowsGentlePrompt() {
        val line = SelectedPeopleInlineLogic.line(RecipientPreviewLogic.summary(emptyList()))
        assertEquals("עדיין לא נבחרו אנשים", line)
    }

    @Test
    fun fewNamesNoRemainder() {
        val line = SelectedPeopleInlineLogic.line(
            RecipientPreviewLogic.summary(listOf("יוסי", "דנה"))
        )
        assertEquals("נשלח ל: יוסי, דנה", line)
    }

    @Test
    fun manyNamesAppendRemainder() {
        val line = SelectedPeopleInlineLogic.line(
            RecipientPreviewLogic.summary(
                labels = listOf("יוסי", "דנה", "רון", "נועה", "אמא", "דוד"),
                max = 3
            )
        )
        assertEquals("נשלח ל: יוסי, דנה, רון, ועוד 3", line)
    }
}
