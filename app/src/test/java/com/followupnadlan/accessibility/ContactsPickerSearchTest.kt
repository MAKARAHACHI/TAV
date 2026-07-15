package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactsPickerSearchTest {
    private val contacts = listOf(
        ContactCandidate(name = "דנה כהן", phone = "050-1234567"),
        ContactCandidate(name = "Avi Levi", phone = "+972 52 999 8888")
    )

    @Test
    fun blankQueryReturnsAllContacts() {
        assertEquals(contacts, ContactsPickerSearch.filter(contacts, " "))
    }

    @Test
    fun filtersByHebrewName() {
        assertEquals(listOf(contacts[0]), ContactsPickerSearch.filter(contacts, "דנה"))
    }

    @Test
    fun filtersByLatinNameIgnoringCase() {
        assertEquals(listOf(contacts[1]), ContactsPickerSearch.filter(contacts, "avi"))
    }

    @Test
    fun filtersByPhoneDigits() {
        assertEquals(listOf(contacts[1]), ContactsPickerSearch.filter(contacts, "9998888"))
    }

    @Test
    fun filtersByPartialName() {
        assertEquals(listOf(contacts[0]), ContactsPickerSearch.filter(contacts, "כה"))
    }

    @Test
    fun filtersByLocalPrefix() {
        // "050" digit-substring matches דנה's 050-... number.
        assertEquals(listOf(contacts[0]), ContactsPickerSearch.filter(contacts, "050"))
    }

    @Test
    fun filtersByInternationalPrefixDigits() {
        // "97252" matches Avi's +972 52 ... via digit-substring.
        assertEquals(listOf(contacts[1]), ContactsPickerSearch.filter(contacts, "97252"))
    }

    @Test
    fun filtersByLocalMobilePrefix054() {
        val list = listOf(
            ContactCandidate(name = "נועה", phone = "054-7654321"),
            ContactCandidate(name = "רון", phone = "03-1112222")
        )
        assertEquals(listOf(list[0]), ContactsPickerSearch.filter(list, "054"))
    }

    @Test
    fun permissionDeniedHasFriendlyMessage() {
        assertEquals(
            "כדי לבחור מאנשי קשר צריך לאשר גישה. אפשר עדיין להוסיף מספר ידנית.",
            ContactsPickerPermissionState.deniedMessage(permissionDenied = true)
        )
        assertNull(ContactsPickerPermissionState.deniedMessage(permissionDenied = false))
    }
}
