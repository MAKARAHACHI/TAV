package com.followupnadlan.templates

data class MessageTemplate(
    val id: String,
    val title: String,
    val body: String,
    /** Optional link to a digital business card; appended to the sent message when non-blank. */
    val cardLink: String = "",
    /** Optional link to a website; appended to the sent message when non-blank. */
    val websiteLink: String = ""
)
