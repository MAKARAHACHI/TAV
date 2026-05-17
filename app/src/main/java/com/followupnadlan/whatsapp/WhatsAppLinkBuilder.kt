package com.followupnadlan.whatsapp

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object WhatsAppLinkBuilder {
    fun build(phone: String, message: String): String {
        val encodedMessage = URLEncoder
            .encode(message, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
        return "https://wa.me/$phone?text=$encodedMessage"
    }
}
