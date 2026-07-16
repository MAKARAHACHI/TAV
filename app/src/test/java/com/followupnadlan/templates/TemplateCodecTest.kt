package com.followupnadlan.templates

import org.junit.Assert.assertEquals
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Test

class TemplateCodecTest {
    @Test
    fun roundTripsRoleAndLinks() {
        val templates = listOf(
            MessageTemplate("m1", "Missed", "missed body", role = TemplateRole.MISSED_CALL),
            MessageTemplate(
                id = "e1",
                title = "Ended",
                body = "ended body",
                cardLink = "https://card",
                websiteLink = "https://site",
                role = TemplateRole.CALL_ENDED
            )
        )

        val decoded = TemplateCodec.decode(TemplateCodec.encode(templates))

        assertEquals(templates, decoded)
        assertEquals(TemplateRole.MISSED_CALL, decoded[0].role)
        assertEquals(TemplateRole.CALL_ENDED, decoded[1].role)
    }

    @Test
    fun legacyLineWithoutRoleFieldDecodesAsCallEnded() {
        // Simulate a pre-role encoded line: id|title|body (5 fields, no role).
        val legacyLine = listOf("id1", "כותרת", "גוף", "", "")
            .joinToString("|") { base64(it) }

        val decoded = TemplateCodec.decode(legacyLine)

        assertEquals(1, decoded.size)
        assertEquals(TemplateRole.CALL_ENDED, decoded[0].role)
        assertEquals("גוף", decoded[0].body)
    }

    @Test
    fun unknownRoleValueDecodesAsCallEnded() {
        val line = listOf("id1", "t", "b", "", "", "SOMETHING_ELSE")
            .joinToString("|") { base64(it) }

        val decoded = TemplateCodec.decode(line)

        assertEquals(TemplateRole.CALL_ENDED, decoded[0].role)
    }

    private fun base64(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
}
