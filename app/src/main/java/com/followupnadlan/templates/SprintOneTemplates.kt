package com.followupnadlan.templates

object SprintOneTemplates {
    val all = listOf(
        MessageTemplate(
            id = "buyer_property_details",
            title = "שליחת פרטי נכס",
            body = """
                שלום,
                שמחתי לדבר איתך לגבי הדירה.
                מצרף כאן את הפרטים שדיברנו עליהם:

                אשמח לתאם סיור בזמן שנוח לך.
            """.trimIndent()
        ),
        MessageTemplate(
            id = "seller_valuation",
            title = "תיאום הערכת נכס",
            body = """
                שלום,
                שמחתי לדבר איתך לגבי הנכס.
                אשמח לתאם פגישה קצרה, להבין את הפרטים, ולהסביר איך אפשר להתקדם בצורה מסודרת.
            """.trimIndent()
        ),
        MessageTemplate(
            id = "missed_call",
            title = "חזרה לשיחה שלא נענתה",
            body = """
                שלום,
                ראיתי שפספסתי את השיחה שלך.
                אפשר לכתוב לי כאן במה מדובר ואחזור אליך בהקדם.
            """.trimIndent()
        )
    )
}
