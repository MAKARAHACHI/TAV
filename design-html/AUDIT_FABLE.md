# FollowUp — ביקורת עומק (FABLE, מבקר חיצוני, קריאה-בלבד)
ענף: `feature/followup-ux-mvp` · תאריך: 2026-07-28 · כל שורה מצוטטת נפתחה בפועל.

---

## 1. תקציר מנהלים

- **טכני:** `AccessibilityApp.kt` = 5,435 שורות, ~40 משתני state בפונקציה אחת, בלי ViewModel. בתוכו ~1,100 שורות UI מת (מסכים שאין אליהם ניווט), ובריפו עוד ~1,650 שורות קבצים מתים (כולל Room DB שלם). ~17% מהקוד מת.
- **§2 הכי חמור:** נתיב האישור-הידני של שיחה-שלא-נענתה נפתח בכלל כ"שיחה שהסתיימה" — הנוטיפיקציה לא שולחת `callType` (ראיה בסעיף 3.1). מציע נוסחים של הרגע הלא-נכון + דגל כרטיס של הרגע הלא-נכון.
- **הבטחות-שקר ב-UI:** טוגל "השהיית שליחה קלה" (2 דקות) לא מחובר לכלום; כפתורי "משתנה חכם" מכניסים `[השם שלי]` שאף קוד לא מחליף — הלקוח יקבל את הסוגריים; תווית "vCard" על פיצ'ר שהוא טקסט.
- **"מקור אמת יחיד" שהוא לא:** `MomentMessageComposer` מוכרז כמקור יחיד, אבל חוקי ההרכבה משוכפלים ב-6 מקומות — וכבר נולד באג (אתר כפול בבורר הנוסחים של השיט).
- **זרימה:** דף עריכת-רגע מציג הגדרות גלובליות (ערוץ, אישור) כאילו הן פר-תרחיש; שינוי ערוץ ב"אחרי שדיברנו" משנה בשקט גם את "אם לא עניתי".
- **מיקוד:** 80% מהערך = שיט האישור + הרגע-הפספוס. שם צריך להשקיע. למחוק: snooze+Room+pipeline, 4 מסכים מתים, 4 דיאלוגים מתים, טוגל ההשהיה, כפתורי התגים.
- **מדד מטעה:** חלק מ-504 הטסטים הירוקים בודקים קוד מת (`FollowUpChannelResolverTest`, `HomeStatusBannerLogicTest`…) ולוגיקת Back שנבדקת אבל לא רצה בפרודקשן.

---

## 2. מטריצת עדיפויות

| # | ממצא | ציר | Impact | Effort | קובץ:שורה |
|---|------|-----|--------|--------|-----------|
| 1 | פרומפט ידני של שיחה-שלא-נענתה נפתח כ"שיחה שהסתיימה" | זרימה/§2 | H | S | MissedCallManualReplyNotificationHelper.kt:64-83 |
| 2 | טוגל "השהיית שליחה קלה" מזויף (לא מחובר) | UI/§2 | H | S | MessageEditorScreen.kt:66,193-199 |
| 3 | תגי `[השם שלי]` לא מוחלפים לעולם — נשלחים כמו-שהם | זרימה/§2 | H | S | MessageEditorScreen.kt:37-40,176 |
| 4 | חוקי הרכבת-הודעה משוכפלים ×6 | טכני | H | M | MomentMessageComposer.kt:6-9 + 5 אתרים |
| 5 | ~2,700 שורות קוד מת (17%) | טכני | H | M | פירוט בסעיף 5 |
| 6 | קובץ-אל 5,435 שורות, 40 state, 24 פרמטרים ×3 | טכני | H | L | AccessibilityApp.kt:266-460,973-1084 |
| 7 | Home: תצוגת "אחרי שדיברנו" בלי חתימה ≠ נשלח | UI/§2 | M | S | AccessibilityApp.kt:2081 |
| 8 | בורר נוסחים בשיט מוסיף שורות-לינק גם כשכרטיס ON → אתר פעמיים | טכני/§2 | M | S | AccessibilityApp.kt:3222 מול 3036-3038 |
| 9 | "טיפלנו ב-N לקוחות" נספר כבר בפתיחת צ'אט | UI/Golden | M | M | HistoryFeed.kt:98-105; FollowUpPromptSender.kt:73 |
| 10 | אונבורדינג: "הכל מוכן! עובדת ברקע" גם אחרי דחיית הרשאות | זרימה/§2 | M | S | OnboardingScreen.kt:102-111,261 |
| 11 | דף-רגע מציג הגדרות גלובליות כפר-תרחיש | זרימה | M | M | AccessibilityApp.kt:438-466,1594-1638 |
| 12 | תווית "vCard" על כרטיס-טקסט | UI | M | S | MessageEditorScreen.kt:188-206 |
| 13 | ⚖️ (עו"ד) קבוע בכותרת הכרטיס לכל מקצוע | UI | M | S | ContactTextCard.kt:40 |
| 14 | אונבורדינג לא אוסף טלפון → כרטיס בלי "לשמירה מהירה" | זרימה | M | S | OnboardingScreen.kt:56-57,173-175 |
| 15 | 3 מערכות צבע מקבילות + 119 hex קשיחים | UI | M | M | AccessibilityTheme.kt:27,173 |
| 16 | ✓✓ כחול + שעות מזויפות ("10:42") בכל תצוגה | UI | L | S | AccessibilityApp.kt:2064,2503-2505 |
| 17 | לוגיקת Back נבדקת ≠ לוגיקת Back שרצה | טכני | M | S | AccessibilityApp.kt:191-199 מול 627-648 |
| 18 | קבוצות-חסומות נערכות ב-2 מסכים עם גייטינג שונה | זרימה | M | S | AccessibilityApp.kt:4701 מול 4975 |
| 19 | יעדי-מגע קטנים ("ערוך"/"מחק" 13sp) באפליקציית נגישות | UI | M | M | AccessibilityApp.kt:1917-1933 |
| 20 | 6 מחלקות SharedPreferences זהות לרגעים | טכני | L | M | EndedCardSettings.kt:18,37,53 |

---

## 3. ממצאים מפורטים

### ציר 1 — טכני

**3.1 [FACT] הרגע הידני של "לא עניתי" מסווג כ"שיחה שהסתיימה".**
בעיה: המסלול המרכזי — פספסת שיחה, מצב ידני → נוטיפיקציה "שיחה שלא נענתה… ממתינה לאישורך" → טאפ → שיט האישור — נפתח עם הכרום, הנוסחים ודגל-הכרטיס של רגע *אחר*.
ראיה: `MissedCallManualReplyNotificationHelper.kt:64-83` — `createPromptIntent` שם רק `EXTRA_PHONE`+`EXTRA_MESSAGE`, בלי `EXTRA_CALL_TYPE`, בלי timestamp, בלי leadName. `FollowUpPromptMode.kt:26-31` — `fromCallType(null)` ⇒ `CALL_ENDED`. בשיט: `AccessibilityApp.kt:3023-3027` — `cardAttached` נלקח מ-`endedCardAttached`; 3014 — בורר הנוסחים מציג נוסחי CALL_ENDED; 2977 — כותרת "🤝 שיחה שהסתיימה"; 3054 — בלי שם ⇒ מספר; 3058 — בלי timestamp ⇒ "עכשיו".
תוצאה: טקסט ההודעה עצמו נכון (ה-message המוכן מנצח), אבל אם כרטיס ה-ended דלוק וה-missed כבוי — ההודעה שתישלח תקבל כרטיס שהמשתמש כיבה לרגע הזה. הפרת §2 של ממש, לא רק קוסמטיקה.
המלצה: להוסיף `putExtra(EXTRA_CALL_TYPE, "missed")` + timestamp + leadName ב-createPromptIntent, וטסט על המיפוי. מאמץ: **S**.

**3.2 [FACT] "מקור אמת יחיד" להרכבה — משוכפל ב-6 מקומות.**
בעיה: `MomentMessageComposer.kt:6-9` מצהיר "Every call site … composes through here". בפועל רק ended/no-answer עוברים דרכו (`EndedFollowUpMessage.kt:29`, `NoAnswerFollowUpMessage.kt:28`). את הכלל "כרטיס ON ⇒ גוף גולמי בלי לינקים; OFF ⇒ לינקים+חתימה" מממשים ידנית גם: Home (`AccessibilityApp.kt:757-759`), שלושת קריאות MomentEditScreen (979, 1018, 1057), שיט האישור (3036-3044, 3071-3075), והנתיב האוטומטי של missed (`MissedCallAutoResponseHandler.kt:513-524` — שגם עושה tag-render שאף אחד אחר לא עושה).
ראיה לנזק שכבר קרה: `AccessibilityApp.kt:3222` — בורר הנוסחים בשיט בונה `MessageComposition.build(variant)` (עם שורות לינק) בלי לבדוק `showCard`, בעוד ה-fallback ב-3036-3038 כן מדכא לינקים כשכרטיס ON ⇒ בחירת נוסח עם כרטיס דלוק שולחת את האתר פעמיים.
המלצה: כל 6 האתרים קוראים ל-`MomentMessageComposer.compose` (כולל וריאנט preview שמחזיר את החלקים בנפרד לציור המסוגנן). מאמץ: **M**.

**3.3 [FACT] קובץ-אל + state שטוח.**
ראיה: `AccessibilityApp.kt:266-460` — ~40 `mutableStateOf` בגוף `AccessibilityApp()`; `MomentEditScreen` עם 24 פרמטרים (1430-1458) משוכפל מילה-במילה 3 פעמים (973-1005, 1012-1044, 1051-1083) — ההבדל היחיד: kind, template, override, כותרת. אין ViewModel/state-holder בכל הריפו.
המלצה: (א) לחלץ `data class MomentEditArgs` + פונקציה אחת שבונה אותם מ-kind — מוחק ~200 שורות מיד (**S**); (ב) לפצל את הקובץ למסך-לקובץ (Home, MomentEdit, PromptSheet, SystemSettings, SmartRules, Pickers) (**M**); (ג) state-holder לכל ההגדרות (**L**).

**3.4 [FACT] לוגיקת Back שנבדקת אינה זו שרצה.**
ראיה: `AccessibilityApp.kt:191-199` — `AccessibilityBackNavigation` (3 שדות) נבדק ב-`AccessibilityBackNavigationTest` אבל אינו נקרא משום מקום בפרודקשן; ה-BackHandler האמיתי (627-648) מממש מחסנית של 8 שכבות ידנית, בלי טסט.
המלצה: למחוק את האובייקט או להזרים דרכו את ה-BackHandler. מאמץ: **S**.

**3.5 [FACT] כפל מערכות scope.**
ראיה: `RecipientScopeSettings.kt:20-41` — `scope` (עם default) ו-`scopeOverride` (nullable) כותבים לאותו KEY עם סמנטיקה שונה. ב-UI נשארו המשתנים הישנים `recipientScope`/`endedScope` (330-332) שמוזנים רק מדיאלוגים מתים (ר' 5). `ExclusionsScreen` מגייט קבוצות-חסומות לפי ה-scope הישן (943→4701) בעוד SmartRules לפי `generalScope` (4975) — שני מסכים יציגו קבוצות שונות לאותו משתמש.
המלצה: למחוק את property‑ה-`scope` הישן + המשתנים המתים; גייטינג אחיד לפי generalScope. מאמץ: **M**.

**3.6 [FACT] `EndedMessageComposer.attachCard` בעצם מצמיד חתימה.**
ראיה: `EndedMessageComposer.kt:20-21` — `if (attachCard) SignatureLine.append(...)`. הקורא היחיד מעביר `attachCard=false` ואז מוסיף חתימה בעצמו (`MomentMessageComposer.kt:46-47`). שם פרמטר שמשקר = באג הבא בהמתנה.
המלצה: inline ולמחוק את הקובץ. מאמץ: **S**.

**3.7 [HYPOTHESIS] טסטים ירוקים כמדד-שווא.**
ראיה עקיפה: קיימים `FollowUpChannelResolverTest`, `HomeStatusBannerLogicTest`, `LastMissedCallerTest`, `HomeQuickWhatsAppNumberLogicTest`, `HomeServiceStatusTest`, `ActivityFeedTest` — כולם על קוד שאין אליו שום נתיב UI (ר' 5). לא הרצתי את הסוויטה (read-only), לכן היפותזה על היחס אך עובדה על קיום הטסטים.
המלצה: מחיקת קוד מת תפיל את הטסטים שלו — זה פיצ'ר, לא רגרסיה.

### ציר 2 — UI

**3.8 [FACT] טוגל "השהיית שליחה קלה" מזויף.**
ראיה: `MessageEditorScreen.kt:66` — `var delaySend by remember { mutableStateOf(true) }`; שורות 193-199 — "ממתין 2 דקות לפני השליחה", דלוק כברירת-מחדל, לא נשמר ולא נקרא בשום מקום. משתמש מאמין שיש לו חלון-חרטה של 2 דקות — אין. הפרת Golden-Rule ישירה.
המלצה: למחוק את השורה ואת ה-Row. מאמץ: **S**.

**3.9 [FACT] "משתנה חכם" שלא קיים.**
ראיה: `MessageEditorScreen.kt:37-40` — התגים `[השם שלי]`/`[תפקיד]`/`[שם העסק]`/`[לינק]`; שורה 176 מדביקה אותם לגוף; `resolveDisplayTags` (217) הוא identity. `TemplateTagRenderer.kt:27-39` מחליף רק `{agent_name}`-וכו', ורק בנתיב missed (`MissedCallAutoResponseHandler.kt:486`). אף קוד לא מטפל בסוגריים-מרובעים ⇒ לקוח מקבל "[השם שלי]" מילולית. גם עורך הפרופיל מבטיח את זה: `AccessibilityApp.kt:4129-4130` — "שם מלא (יופיע ב-[השם שלי])".
המלצה: או למחוק את כפתורי התגים ואת ההבטחות בתוויות (S), או לממש רינדור בכניסה ל-`MomentMessageComposer` (M). כרגע — למחוק.

**3.10 [FACT] תווית vCard כוזבת.** `MessageEditorScreen.kt:188` — "לצרף כרטיס ביקור (vCard)… שולח איש קשר לשמירה מהירה בטלפון". המוצר החליט: כרטיס-טקסט, לא vCard (WhatsApp חוסם קובץ למספר לא-שמור). התווית מבטיחה איש-קשר-נשמר; נשלח טקסט. תיקון copy. **S**.

**3.11 [FACT] Home מציג את "אחרי שדיברנו" בלי חתימה — אבל היא נשלחת.**
ראיה: `AccessibilityApp.kt:2081` — `signature = ""` רק לכרטיס ה-ended (missed ו-no-answer מקבלים `signature`); נתיב השליחה `MomentMessageComposer.kt:44-47` מצמיד חתימה כשכרטיס OFF. תצוגת Home ≠ נשלח עבור הרגע הזה. **S** (להעביר `signature`).

**3.12 [FACT] ✓✓ כחול ושעות בדויות.** "10:42"/"11:05"/"12:30" + ✓✓ בצבע-נקרא בכל הבועות (2064, 2081, 2098, 1542-1543, 2503-2505, `MessageEditorScreen.kt:152`). ההערה בקוד מודה "no WhatsApp receipt access" — אבל למשתמש זה נראה "נמסר ונקרא". באפליקציה שדגלה "כנות מעל תחכום" — להוריד את ה-✓✓ או לאפור. **S**.

**3.13 [FACT] ⚖️ קבוע.** `ContactTextCard.kt:40` — `HEADER_EMOJI = "⚖️"` (מאזני-משפט) לכל משתמש; קהל היעד המוצהר: מתווכים. אין שדה מקצוע/אימוג'י בפרופיל. **S** (🔹/🏠 ניטרלי או נגזר-עיסוק).

**3.14 [FACT] רף נגישות נמוך לאפליקציה בשם הזה.** "ערוך"/"מחק" כטקסט 13sp קליקבילי (`AccessibilityApp.kt:1917-1933`), "ערוך רשימה ›" (1758), "הפעל נגישות ›" (1656) — יעדי מגע < 48dp; אימוג'י כאייקונים בלי contentDescription (2180, 2323); Switch בלי תיאור-מצב. פונטים ננעלים ב-sp קבוע קטן (10-13sp). **M**.

**3.15 [FACT] שלוש מערכות צבע.** `AccessibilityTheme.kt:27` (`AccessibilityColors`) + 173 (`AccessibilityExtra.colors`) + 119 מופעי `Color(0x…)` ישירים בחבילה (54 בקובץ הראשי, למשל 1496, 1511, 2016, 2984-2986 שמגדיר פלטת-שיט רביעית). שינוי צבע מותג = חיפוש-והחלפה עיוור. **M**.

**3.16 [FACT] קופי כפול ב-SmartRules.** 4941 "מי מקבל הודעות המשך (ברירת מחדל)" ואז 4944 subtitle "מי מקבל הודעות ברירת מחדל" — אותו משפט פעמיים. וכן 📷 מזויף בעורך הפרופיל (4110-4119 — "drawn as designed, not wired") ושורות הרשאה עם `onRequest = null` שלא ניתנות לפעולה (4157-4170). **S**.

### ציר 3 — זרימת משתמש

**3.17 [FACT] דף-רגע מוכר הגדרות גלובליות כפר-תרחיש.**
ראיה: כותרת הכרטיס "הגדרות תרחיש וערוץ" (1594); "ערוץ שליחה" כותב `FollowUpChannelSettings.apply` הגלובלי (438-454); "אישור לפני שליחה" כותב `settings.whatsappMode` הגלובלי (460-466). המשתמש בדף "אחרי שדיברנו" בוחר SMS — וגם "אם לא עניתי" עובר ל-SMS בלי שום חיווי. לטובת ההגינות: לשני הקולדאונים דווקא כתוב "חל על כל התרחישים" (1703, 1718) — לערוץ ולאישור לא.
המלצה: או תוספת "· חל על כל התרחישים" לערוץ (S), או ערוץ פר-רגע אמיתי (L). לפחות ה-copy. מאמץ: **S-M**.

**3.18 [FACT] אונבורדינג מכריז הצלחה ללא תלות בתוצאה.**
ראיה: `OnboardingScreen.kt:102-111` — כפתור שלב-3 משגר את בקשת ההרשאות וקופץ מיידית ל-slide 4; שורה 261 — "המערכת מוגדרת ועובדת ברקע" גם אם המשתמש הקיש "דחה". ה-callback ב-`AccessibilityApp.kt:655-665` מפעיל את השירות רק אם שתי ההרשאות ניתנו — אחרת כלום, והמסך עדיין חוגג. המשתמש יגלה רק אם יבחין ב-⚠️ בבית.
המלצה: להמתין לתוצאת ה-launcher; slide 4 עם שני מצבים ("הכל מוכן" / "חסרה הרשאה — הפעל"). מאמץ: **S**.

**3.19 [FACT] אונבורדינג לא אוסף טלפון.**
ראיה: `OnboardingScreen.kt:56-57,173-175` — רק שם+תפקיד. `ContactTextCard.build` (69-72) משמיט את בלוק "לשמירה מהירה" ואת ההנחיה בלי טלפון — כלומר הפיצ'ר המרכזי של הכרטיס (שמור-אותי-באנשי-קשר) מת מהתקנה ועד שהמשתמש ימצא לבד את עורך הפרופיל. [HYPOTHESIS] רוב המשתמשים לא ימצאו: `HomeWarning.EMPTY_PROFILE` נדלק רק כשהחתימה ריקה לגמרי (`HomeWarning.kt:32-37`), ושם ניתן באונבורדינג ⇒ אין שום התראה.
המלצה: שדה טלפון בשלב 2 (עם prefill ממספר המכשיר אם אפשר). מאמץ: **S**.

**3.20 [FACT] "טיפלנו ב-N לקוחות היום!" נספר בפתיחת צ'אט.**
ראיה: `HistoryFeed.kt:98-105` — `WHATSAPP_REPLY_OPENED` נחשב client-facing send; `FollowUpPromptSender.kt:73` רושם אותו מיד כשהצ'אט נפתח ("so the send counts even before the service confirms"); Home מציג "טיפלנו" (2019). בלי שירות-נגישות, אם המשתמש חוזר בלי להקיש שלח — הלקוח לא קיבל כלום והבדג' ספר אותו. [HYPOTHESIS] שכיחות בפועל; [FACT] המנגנון.
המלצה: או ניסוח "הכנו מענה ל-N" או ספירת `*_SENT` בלבד כשנגישות דלוקה. מאמץ: **M**.

**3.21 [FACT] עריכה בשיט נזרקת בלי אזהרה.** "ביטול עריכה" (`AccessibilityApp.kt:3264-3267`) מוחק את הטיוטה בלי אישור; אין "שמור כנוסח". מקובל ל-MVP אך ראוי ל-Undo-Snackbar כמו שכבר קיים ברשימות (700-714). **S**.

### ציר 4 — מיקוד

**3.22 להכפיל השקעה:** (א) שיט האישור — הוא רגע-האמון היחיד שהמשתמש פוגש כל יום; לתקן 3.1 + 3.20 לפני הכל. (ב) נתיב missed-ידני מקצה-לקצה עם טסט אינטגרציה אחד (נוטיפיקציה→שיט→שליחה→לוג). (ג) איחוד ההרכבה (3.2) — הוא שממילא יקטין את מחיר כל פיצ'ר עתידי.

**3.23 מה לא להוסיף:** אין להוסיף אף select/טוגל חדש לדף-רגע לפני שמורידים אחד — הדף כבר מכיל 7 בקרות ו-3 מהן גלובליות-בתחפושת.

---

## 4. כפילויות (קונקרטי)

1. **חוקי הרכבת הודעה ×6** — MomentMessageComposer.kt:38-52; AccessibilityApp.kt:757-759, 979, 1018, 1057, 3036-3044+3071-3075; MissedCallAutoResponseHandler.kt:513-524. (הבאג ב-3222 נולד מזה.)
2. **בועת WhatsApp-preview ×4 מימושים** — hero של דף-רגע (1509-1546), אקורדיון Home (2462-2506), שיט האישור (3148-3208), MessageEditorScreen (122-156). אותו כלל "כרטיס מחליף חתימה" מועתק בכל אחת (1524-1539, 2477-2498, 3180-3193, MessageEditorScreen.kt:135-149) — בדיוק ה"guard בארבעה מקומות" שחשדתם בו. חילוץ `WhatsAppBubble(body, signature, card, meta)` אחד.
3. **קריאת MomentEditScreen ×3** — 973-1005 / 1012-1044 / 1051-1083: ~30 שורות זהות פר-רגע, שוני רק ב-kind/template/override.
4. **עריכת קבוצות-חסומות ×2** — ExclusionsScreen (4706-4716, RadioRow) ו-SmartRulesScreen (4976-4987, Switch) כותבים לאותו `ExclusionsStore` עם קונטרול שונה וגייטינג שונה (3.5).
5. **6 מחלקות prefs זהות** — EndedCardSettings/MissedCardSettings/NoAnswerCardSettings (EndedCardSettings.kt:18,37,53) + RecipientScope/EndedScope/NoAnswerScope — אותו boilerplate; מחלקה אחת עם suffix פר-רגע.
6. **מיפוי ask↔whatsappMode ×3** — SettingsScreen (830-837, מת), askPicker (1302-1314, מת), MomentApprovalModeMapper (239-246, חי). שלוש אמיתות לאותו דגל.
7. **פלטות בועה ×2** — PromptSheetBg/PromptChatBg/PromptBubbleGreen (2981-2986) מול אותם צבעים inline ב-Home/עריכה (2459, 2463, 1496, 1511).

---

## 5. מה למחוק (עם נימוק)

**בתוך AccessibilityApp.kt (~1,100 שורות, אפס שינוי התנהגות):**
- `BottomNav`+`NavItem` (1354-1387) — אין ניווט תחתון ב-MVP-1 (ההערה ב-1344-1347 מודה).
- ענפי `AccessibilityTab.ACTIVITY`/`SETTINGS` (812-865) — `tab` לעולם לא משתנה מ-HOME (רק BackHandler מאפס אליו). איתם: `ActivityScreen`+`ActivityRowItem` (3329-3403), `SettingsScreen` (3406-3626), `MyDetailsInlineCard`+`MyDetailRow` (3627-3765).
- `TemplatesScreen`+`TemplateCard`+`TemplateCardEditor` (2702-2974) + `TemplateRoleToggle`+`WhatsAppMessagePreview` (2537-2596) — נפתחים רק מ-SettingsScreen המת.
- `ContactCardScreen`+`ContactCardPreview` (4249-4440) — `AccessibilityModal.CONTACT_CARD` לא מוצב בשום מקום.
- 4 בלוקי `JourneyOptionPickerDialog` + הדגלים שלהם (1231-1318; `recipientPickerOpen`/`askPickerOpen`/`channelPickerOpen`/`endedScopePickerOpen` לעולם לא true) + הדיאלוג עצמו (1389-1416) + המשתנים `recipientScope`/`endedScope` (330-332).
- עוזרי `homeServiceStatus*` (2598-2634) — אפס קריאות.
- imports מתים: `ContactCardShareResult`, `PrepareAndShareContactCard` (110-111).

**קבצים שלמים (~1,650 שורות):**
- `snooze/` (4 קבצים) + `data/` (Room: AppDatabase, LeadDao/Entity, FollowUpTaskDao/Entity) — ReminderWorker הוא הצרכן היחיד של ה-DB ואף אחד לא קורא ל-ReminderScheduler. מוחק גם את תלות Room מה-build.
- `pipeline/LeadPipeline.kt` (154) — אפס שימוש.
- `postcall/PostCallCard.kt`, `accessibility/HomeStatusBanner.kt`, `LastMissedCaller.kt`, `HomeQuickWhatsAppNumber.kt`, `HomeServiceStatus.kt`, `ActivityFeed.kt` — שרידי ה-Home הישן.
- `setup/SelfTestChecker.kt`, `SetupReadinessLogic.kt`, `OemGuidance.kt` — לא מחוברים.
- `accessibility/FollowUpChannelResolver.kt` — נצרך רק ע"י הטסט של עצמו.
- `sharing/` (3 קבצים) + `profile/VCardBuilder.kt` + `ContactCardFileWriter.kt` — פיצ'ר ה-vCard לא חי בענף הזה; אם מתוכנן — להשאיר בענף הניסוי, לא כאן. [HYPOTHESIS: מתוכנן לשוב; לוודא מול בעל-המוצר.]
- `templates/TemplateTags.kt` (insertion logic + supported list) — אין UI שמשתמש.

**פיצ'רים-בתחפושת למחוק מה-UI:** טוגל ההשהיה (3.8), כפתורי התגים (3.9), 📷 (3.16), ✓✓ (3.12).

---

## 6. שלושה דברים למחר בבוקר

1. **לתקן את סיווג הרגע בפרומפט הידני** — `putExtra(EXTRA_CALL_TYPE, "missed")` + timestamp + leadName ב-`MissedCallManualReplyNotificationHelper.createPromptIntent` (שעה עבודה, סוגר את הפרת ה-§2 הגדולה ביותר).
2. **מחיקת השקרים הקטנים** — טוגל ההשהיה, כפתורי `[השם שלי]`, תווית vCard, `signature=""` של ended ב-Home, MessageComposition בבורר-הנוסחים של השיט (3222→לכבד showCard). חצי יום, כל אחד S.
3. **מחיקת הקוד המת** (סעיף 5) — יום עבודה, ‑17% קוד, ‑Room, קובץ-האל יורד ל~4,300 עוד לפני הפיצול, וטסטים מפסיקים לשקר.

---

## 7. שאלות פתוחות

1. vCard-share (sharing/, VCardBuilder) — חוזר בקרוב או למחוק גם כאן?
2. ערוץ שליחה — מוצרית: גלובלי או פר-רגע? (הקוד גלובלי, ה-UI רומז פר-רגע.)
3. "טיפלנו ב-N" — מותר לספור פתיחת-צ'אט, או רק שליחה מאושרת?
4. ✓✓ בפריוויו — נשאר (מטאפורת-וואטסאפ) או מוסר (כנות)?
5. ⚖️ — אימוג'י ניטרלי קבוע, או שדה עיסוק-אימוג'י בפרופיל?
6. Room נמחק — יש תוכנית DB ל-MVP-2 שמצדיקה להשאיר סכימה?
7. ActivityScreen/"היום" — חוזר אי-פעם כטאב, או ש-HistoryScreen מספיק?
