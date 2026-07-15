package com.followupnadlan.accessibility

import androidx.compose.ui.text.input.KeyboardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AllowedRecipientsStoreTest {
    @Test
    fun manualNumberAcceptsIsraeliLocalAndInternationalForms() {
        val local = AllowedRecipientValidator.validEntry(
            AllowedRecipient(label = "0521234567", number = "0521234567")
        )
        val international = AllowedRecipientValidator.validEntry(
            AllowedRecipient(label = "+972521234567", number = "+972521234567")
        )

        assertEquals("972521234567", local?.number)
        assertEquals("972521234567", international?.number)
    }

    @Test
    fun invalidManualNumberFailsValidation() {
        assertNull(
            AllowedRecipientValidator.validEntry(
                AllowedRecipient(label = "bad", number = "not a phone")
            )
        )
    }

    @Test
    fun matcherAllowsEquivalentLocalAndInternationalNumber() {
        val entries = listOf(AllowedRecipient(label = "Dana", number = "+972521234567"))

        assertTrue(AllowedRecipientMatcher.isAllowed(entries, "0521234567"))
        assertFalse(AllowedRecipientMatcher.isAllowed(entries, "0509998888"))
    }

    @Test
    fun allowedListManualFieldUsesPhoneKeyboard() {
        assertEquals(KeyboardType.Phone, AllowedRecipientsUiSpec.phoneKeyboardType)
        assertNotNull(AllowedRecipientsUiSpec.TITLE)
    }

    @Test
    fun recipientModeUsesOnlySelectedInsteadOfAnyNumberExceptContacts() {
        val names = RecipientScope.values().map { it.name }

        assertTrue(names.contains("ANY_NUMBER"))
        assertTrue(names.contains("CONTACTS_ONLY"))
        assertTrue(names.contains("NON_CONTACTS_ONLY"))
        assertTrue(names.contains("ONLY_SELECTED"))
        assertFalse(names.contains("ANY_NUMBER_EXCEPT_CONTACTS"))
    }

    @Test
    fun contradictoryBlockGroupsAreHiddenForRecipientModes() {
        assertFalse(RecipientRulesUi.visibleBlockGroups(RecipientScope.CONTACTS_ONLY).contains(BlockedRecipientGroup.CONTACTS))
        assertFalse(RecipientRulesUi.visibleBlockGroups(RecipientScope.NON_CONTACTS_ONLY).contains(BlockedRecipientGroup.NON_CONTACTS))
        assertTrue(RecipientRulesUi.visibleBlockGroups(RecipientScope.ONLY_SELECTED).isEmpty())
    }
}
