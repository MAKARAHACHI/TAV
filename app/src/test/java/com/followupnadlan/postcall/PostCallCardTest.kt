package com.followupnadlan.postcall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostCallCardTest {
    @Test
    fun cards_areExactlyTheApprovedFourInOrder() {
        val cards = PostCallCards.all

        assertEquals(4, cards.size)
        assertEquals(
            listOf(
                "שיחה קרה נפתחה",
                "שיחה קרה נסגרה בנימוס",
                "קונה מתעניין",
                "צריך לחזור אליו"
            ),
            cards.map { it.title }
        )
        assertEquals(
            listOf(
                "בעל נכס הסכים להמשך",
                "להשאיר דלת פתוחה",
                "לשלוח פרטי נכס",
                "רק תזכורת — בלי הודעה"
            ),
            cards.map { it.subtitle }
        )
    }

    @Test
    fun cards_haveStableUniqueIds() {
        val ids = PostCallCards.all.map { it.id }

        assertEquals(
            listOf(
                PostCallCards.COLD_CALL_OPENED_ID,
                PostCallCards.COLD_CALL_CLOSED_POLITELY_ID,
                PostCallCards.BUYER_INTERESTED_ID,
                PostCallCards.CALL_BACK_NEEDED_ID
            ),
            ids
        )
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun cards_haveDeterministicComposerHints() {
        PostCallCards.all.forEach { card ->
            assertTrue(card.title.isNotBlank())
            assertTrue(card.subtitle.isNotBlank())
            assertTrue(card.composerHint.templateId.isNotBlank())
            assertTrue(card.composerHint.initialMessage.isNotBlank())
        }

        assertEquals("seller_valuation", PostCallCards.all[0].composerHint.templateId)
        assertEquals("seller_valuation", PostCallCards.all[1].composerHint.templateId)
        assertEquals("buyer_property_details", PostCallCards.all[2].composerHint.templateId)
        assertEquals("missed_call", PostCallCards.all[3].composerHint.templateId)
    }

    @Test
    fun findById_returnsMatchingCardOnly() {
        assertEquals(
            "קונה מתעניין",
            PostCallCards.findById(PostCallCards.BUYER_INTERESTED_ID)?.title
        )
        assertNotNull(PostCallCards.findById(PostCallCards.CALL_BACK_NEEDED_ID))
        assertEquals(null, PostCallCards.findById("unknown"))
    }
}
