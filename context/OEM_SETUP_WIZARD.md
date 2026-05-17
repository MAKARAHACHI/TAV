# OEM Setup Wizard

## Purpose

Android OEMs may restrict background services and notifications. The setup wizard reduces support issues before users rely on the app.

## Wizard steps

### Step 1: Welcome

```txt
ברוך הבא ל-FollowUp Nadlan.
האפליקציה עוזרת לך לא לשכוח המשך טיפול אחרי שיחות נדל״ן.
```

### Step 2: Permissions

Request/explain:

- Phone state.
- Call log.
- Notifications.
- Optional contacts.

Do not request all at once without explanation.

### Step 3: Agent profile

Fields:

- full name.
- business name.
- phone.
- website.
- signature.

### Step 4: Battery/OEM guidance

Detect manufacturer and show relevant guidance.

Samsung copy:

```txt
במכשירי Samsung ייתכן שחיסכון בסוללה יעצור את הזיהוי אחרי זמן.
פתח הגדרות סוללה והחרג את האפליקציה מחיסכון.
```

Xiaomi/Redmi copy:

```txt
במכשירי Xiaomi/Redmi צריך לאשר Autostart ולבטל חיסכון סוללה לאפליקציה.
```

Generic copy:

```txt
כדי שהאפליקציה תזהה סיום שיחה, מומלץ לאפשר לה לפעול ברקע.
```

### Step 5: Self-test

```txt
בדיקת עבודה
חייג למספר כלשהו, נתק אחרי כמה שניות, ובדוק אם כרטיס המשך השיחה הופיע.
```

Buttons:

```txt
התחל בדיקה
זה עבד
זה לא עבד
```

If failed, guide based on missing conditions.

## Self-test checks

Check:

- permissions granted;
- notification permission granted;
- service running;
- last call event observed;
- battery optimization known status if accessible;
- call log returned recent row if permission granted.

## Settings screen

Include:

```txt
בדוק שהמוצר עובד
הרשאות
הגדרות סוללה
תבנית ברירת מחדל
משך שיחה מינימלי לפתיחת כרטיס
```

## Anti-patterns

Do not:

- skip setup wizard;
- bury self-test in advanced settings only;
- assume Samsung/Xiaomi will keep service alive;
- blame the user when OEM settings block the app;
- force contact permission before product works.
