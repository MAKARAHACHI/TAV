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
        ),
        MessageTemplate(
            id = "missed_call_auto_response",
            title = "תגובה אוטומטית לשיחה שלא נענתה",
            body = """
                שלום, תודה שפניתם ל{{businessName}}.
                אנחנו כרגע בשטח או בעבודה ולכן לא תמיד יכולים לענות מיד.
                קיבלנו את פנייתכם ונחזור אליכם בהקדם.
                אפשר לשלוח כאן בקצרה מה צריך לבצע או לצרף תמונה.
            """.trimIndent()
        )
    )
}
