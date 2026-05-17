package com.followupnadlan.whatsapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppLinkBuilderTest {
    @Test
    fun buildsWaMeLinkWithEncodedMessage() {
        val link = WhatsAppLinkBuilder.build("972501234567", "שלום עולם")

        assertTrue(link.startsWith("https://wa.me/972501234567?text="))
        assertTrue(link.contains("%D7%A9%D7%9C%D7%95%D7%9D"))
    }

    @Test
    fun encodesSpacesAsPercentTwenty() {
        assertEquals(
            "https://wa.me/972501234567?text=hello%20world",
            WhatsAppLinkBuilder.build("972501234567", "hello world")
        )
    }
}
