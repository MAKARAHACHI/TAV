package com.followupnadlan.postcall

data class PostCallComposerHint(
    val templateId: String,
    val initialMessage: String
)

data class PostCallCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val composerHint: PostCallComposerHint
)

object PostCallCards {
    const val COLD_CALL_OPENED_ID = "cold_call_opened"
    const val COLD_CALL_CLOSED_POLITELY_ID = "cold_call_closed_politely"
    const val BUYER_INTERESTED_ID = "buyer_interested"
    const val CALL_BACK_NEEDED_ID = "call_back_needed"

    val all: List<PostCallCard> = listOf(
        PostCallCard(
            id = COLD_CALL_OPENED_ID,
            title = "שיחה קרה נפתחה",
            subtitle = "בעל נכס הסכים להמשך",
            composerHint = PostCallComposerHint(
                templateId = "seller_valuation",
                initialMessage = """
                    היי {lead_name}, כאן {agent_name} מ{office_name}.
                    תודה על השיחה.
                    כמו שסיכמנו, אשלח לך את כרטיס הביקור שלי.
                    אתאם ביקור בנכס ואחזור אליך אחרי שאראה אותו.

                    {business_card}
                """.trimIndent()
            )
        ),
        PostCallCard(
            id = COLD_CALL_CLOSED_POLITELY_ID,
            title = "שיחה קרה נסגרה בנימוס",
            subtitle = "להשאיר דלת פתוחה",
            composerHint = PostCallComposerHint(
                templateId = "seller_valuation",
                initialMessage = """
                    היי {lead_name}, כאן {agent_name} מ{office_name}.
                    תודה שענית.
                    מבין שהעיתוי לא מתאים עכשיו.
                    אם בהמשך תרצה לשמוע רעיון או לבדוק כיוון נוסף - אני כאן.

                    {business_card}
                """.trimIndent()
            )
        ),
        PostCallCard(
            id = BUYER_INTERESTED_ID,
            title = "קונה מתעניין",
            subtitle = "לשלוח פרטי נכס",
            composerHint = PostCallComposerHint(
                templateId = "buyer_property_details",
                initialMessage = """
                    היי {lead_name}, כאן {agent_name} מ{office_name}.
                    תודה על הפנייה.
                    הנה פרטי הנכס שדיברנו עליו:

                    {property_link}

                    אפשר לתאם לך ביקור.
                    מתי נוח לך?
                """.trimIndent()
            )
        ),
        PostCallCard(
            id = CALL_BACK_NEEDED_ID,
            title = "צריך לחזור אליו",
            subtitle = "רק תזכורת — בלי הודעה",
            composerHint = PostCallComposerHint(
                templateId = "missed_call",
                initialMessage = """
                    היי, כאן {agent_name} מ{office_name}.
                    ניסיתי להתקשר ולא הצלחתי לתפוס.
                    אחזור אליך בהמשך, או שאתה מוזמן לחזור אליי.
                """.trimIndent()
            )
        )
    )

    fun findById(id: String): PostCallCard? = all.firstOrNull { it.id == id }
}
