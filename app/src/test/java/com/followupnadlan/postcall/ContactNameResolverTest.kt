package com.followupnadlan.postcall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactNameResolverTest {
    @Test
    fun firstNameFromTwoPartName() {
        assertEquals("Dana", ContactNameResolverLogic.extractFirstName("Dana Cohen"))
    }

    @Test
    fun firstNameFromHebrewName() {
        assertEquals("דני", ContactNameResolverLogic.extractFirstName("דני כהן"))
    }

    @Test
    fun trimsExtraWhitespace() {
        assertEquals("Dana", ContactNameResolverLogic.extractFirstName("  Dana   Cohen  "))
    }

    @Test
    fun singleNameIsAccepted() {
        assertEquals("Dana", ContactNameResolverLogic.extractFirstName("Dana"))
    }

    @Test
    fun blankNameReturnsNull() {
        assertNull(ContactNameResolverLogic.extractFirstName("   "))
    }

    @Test
    fun nullNameReturnsNull() {
        assertNull(ContactNameResolverLogic.extractFirstName(null))
    }
}
