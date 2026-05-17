# Template Engine

## Purpose

The template engine lets a real-estate agent send professional WhatsApp messages quickly without writing from zero.

It supports:

- built-in Hebrew templates;
- user-edited templates;
- placeholders;
- quick message blocks;
- agent profile insertion;
- final editable message.

## MVP template count

Start with exactly 12 built-in templates. Do not overwhelm users.

## Built-in template categories

### Buyer

1. תודה על השיחה + שליחת פרטי נכס.
2. תיאום סיור בדירה.
3. שליחת נכסים דומים.
4. בדיקת תקציב / מימון.

### Seller

5. תודה על השיחה + הצעת בדיקת שווי.
6. תיאום פגישת הערכת נכס.
7. הסבר קצר על תהליך מכירה.

### Investor

8. שליחת נתוני תשואה.
9. שליחת הזדמנות השקעה.

### Missed / unanswered

10. ניסיתי להשיג אותך.
11. ראיתי שהתעניינת בנכס.

### After meeting

12. תודה על הפגישה + המשך פעולה.

## Example templates

### Buyer: property details

```txt
שלום {lead_name},
שמחתי לדבר איתך על הדירה ב{neighborhood}.
מצרף לך כאן את הפרטים:
{property_link}

אם מתאים, נוכל לתאם סיור ל{suggested_time}.

{signature}
```

### Seller: valuation meeting

```txt
שלום {lead_name},
שמחתי לדבר איתך לגבי הנכס שלך.
אשמח לתאם איתך בדיקת שווי קצרה ולהסביר איך אפשר להתקדם בצורה מסודרת.

{signature}
```

### Missed call

```txt
שלום {lead_name},
ראיתי שפספסתי את השיחה שלך.
אפשר לכתוב לי כאן במה מדובר ואחזור אליך בהקדם.

{signature}
```

## Placeholder rendering

Supported placeholders are defined in `context/DATA_CONTRACTS.md`.

Rules:

- Replace known placeholders.
- Leave unknown placeholders visible.
- Do not silently delete unknown placeholders.
- Let user edit final text before WhatsApp.

## Block composer

The block composer creates a message from parts.

Groups:

```txt
פתיחה
משפט הקשר
פרטי נכס
קריאה לפעולה
סיום
חתימה
```

Example UI:

```txt
פתיחה:
[שלום {lead_name}, שמחתי לדבר איתך]
[היי {lead_name}, תודה על השיחה]
[שלום, בהמשך לשיחתנו]

נושא:
[מצרף פרטים על הדירה]
[רוצה לתאם סיור]
[שולח לך כמה אפשרויות]

סיום:
[מתי נוח לך לדבר?]
[אפשר לתאם להיום/מחר]
[אני זמין לכל שאלה]
```

## User template editing

MVP supports:

- edit built-in template as copy;
- create new template;
- mark default template;
- delete user-created template;
- reset built-ins optional later.

## Anti-patterns

Do not build:

- AI template writing in MVP;
- cloud template marketplace;
- complex conditional logic;
- CRM campaign sequences;
- mass messaging.
