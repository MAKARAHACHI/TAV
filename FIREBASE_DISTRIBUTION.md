# הפצת פולואפ דרך Firebase App Distribution

הפצה לסוכנים בלי Google Play. כל APK חייב להיות חתום באותו keystore
(`keystore/followup-release.jks`) — אחרת עדכונים נשברים. ראה `RELEASE_SIGNING.md`.

## פרטי הפרויקט
- Firebase project: `followup-app-il`
- Android App ID: `1:711103587669:android:f8a2768d6c5b7faa1c3bdc`
- Package: `com.followupnadlan`
- Console: https://console.firebase.google.com/project/followup-app-il/appdistribution

## שליחת גרסה חדשה (עדכון) — פקודה אחת
1. בנה APK חתום:
   ```
   cd /f/followup && ./gradlew :app:assembleRelease
   ```
2. העלה + הפץ:
   ```
   firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk \
     --app 1:711103587669:android:f8a2768d6c5b7faa1c3bdc \
     --project followup-app-il \
     --release-notes "מה חדש בגרסה הזאת" \
     --groups "pilot"
   ```
כל הבודקים בקבוצה מקבלים התראה באפליקציית "App Tester" — לוחצים ומתעדכנים.

## חשוב: להעלות versionCode לכל עדכון
Android לא מתקין עדכון עם אותו versionCode. לפני כל הפצה חדשה, העלה
`versionCode` ב-`app/build.gradle.kts` (1 → 2 → 3 ...). אפשר גם `versionName`
("0.1.0" → "0.1.1") לתצוגה.

## הוספת סוכנים (בודקים)
בקונסול: App Distribution → Testers & Groups → צור קבוצה `pilot` → הוסף
מיילים של סוכנים. או דרך CLI:
```
firebase appdistribution:testers:add "agent@example.com" --project followup-app-il
```
כל סוכן: מקבל מייל → מתקין אפליקציית "App Tester" (פעם אחת) → משם מוריד את פולואפ.
הסוכן לא צריך שום סיסמה שלך — רק המייל שלו.

## מה הסוכן חווה
1. מייל-הזמנה מ-Firebase.
2. התקנת "App Tester" (אפליקציה חינמית של Google, פעם אחת).
3. מתקין פולואפ משם. עדכונים עתידיים = התראה ב-App Tester.
