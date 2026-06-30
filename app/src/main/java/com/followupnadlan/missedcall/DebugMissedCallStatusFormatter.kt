package com.followupnadlan.missedcall

object DebugMissedCallStatusFormatter {
    fun format(action: MissedCallAutoResponseAction): String =
        when (action) {
            MissedCallAutoResponseAction.ATTEMPT_WHATSAPP_AUTO_SEND ->
                "אירוע הבדיקה נשלח. WhatsApp אמור להיפתח, והאפליקציה תנסה שליחה אוטומטית אם שירות הנגישות פעיל."
            MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP ->
                "אירוע הבדיקה נשלח. WhatsApp אמור להיפתח עם הודעה מוכנה לשליחה."
            MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP_ACCESSIBILITY_MISSING ->
                "אירוע הבדיקה נשלח. שירות הנגישות לא פעיל, לכן WhatsApp ייפתח עם הודעה מוכנה לשליחה ידנית."
            MissedCallAutoResponseAction.SEND_AUTOMATIC_SMS ->
                "אירוע הבדיקה נשלח, אבל לפי ההגדרות הנוכחיות הפעולה הצפויה היא SMS ולא WhatsApp."
            MissedCallAutoResponseAction.OPEN_MANUAL_FALLBACK ->
                "אירוע הבדיקה נשלח, אבל WhatsApp או SMS אוטומטי לא זמינים כרגע. אמורה להופיע נפילה ידנית."
            MissedCallAutoResponseAction.SKIP_DISABLED ->
                "אירוע הבדיקה נשלח, אבל תגובת שיחה שלא נענתה כבויה כרגע ולכן WhatsApp לא ייפתח."
            MissedCallAutoResponseAction.SKIP_DUPLICATE ->
                "אירוע הבדיקה נשלח, אבל ה-cooldown חסם את המספר הזה. נסה מספר אחר או המתן לסיום חלון הקירור."
            MissedCallAutoResponseAction.SKIP_NO_NUMBER ->
                "אירוע הבדיקה נשלח בלי מספר תקין, ולכן הוא נחסם לפני פתיחת WhatsApp."
            MissedCallAutoResponseAction.SKIP_NO_PERMISSION ->
                "אירוע הבדיקה נשלח, אבל אין כרגע ערוץ מותר לביצוע. בדוק הרשאות וערוץ תגובה."
            MissedCallAutoResponseAction.SKIP_NOT_MISSED_CALL ->
                "אירוע הבדיקה לא עומד בתנאי שיחה נכנסת שלא נענתה, ולכן לא תתבצע תגובה."
            MissedCallAutoResponseAction.SKIP_NO_TEMPLATE ->
                "אירוע הבדיקה נשלח, אבל חסרה תבנית תגובה לשיחה שלא נענתה ולכן WhatsApp לא ייפתח."
            MissedCallAutoResponseAction.SKIP_EXCLUDED ->
                "אירוע הבדיקה נשלח, אבל המספר נמצא ברשימת מי שלא לשלוח לו, ולכן לא ייפתח WhatsApp ולא יישלח SMS."
            MissedCallAutoResponseAction.SKIP_CONTACTS_ONLY_UNVERIFIED ->
                "אירוע הבדיקה נשלח, אבל במצב אנשי קשר בלבד לא ניתן לוודא שהמספר שמור (חסרה הרשאת אנשי קשר או שהמספר לא ברשימה), ולכן לא ייפתח WhatsApp ולא יישלח SMS."
        }
}
