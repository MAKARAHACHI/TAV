# External Distribution

## Decision

Version 1 is distributed outside Google Play as a signed APK.

Reason:

- The product needs to test `READ_CALL_LOG` for automatic last-number detection.
- Google Play policy makes Call Log permission difficult/impossible for this app category.
- External distribution is acceptable for controlled early marketing and beta.

## Trust rule

External distribution increases user friction and fear. The app must over-communicate trust:

- Signed APK.
- Clear website.
- Clear privacy explanation.
- No automatic sending.
- Local-only storage.
- Simple uninstall path.
- Human support contact.

## Landing page must include

- What the app does.
- Why APK installation is required.
- How to install safely.
- Required permissions and why.
- Privacy statement.
- Price/trial.
- Changelog.
- Download button.

## Install flow copy

```txt
1. הורד את קובץ ההתקנה.
2. אשר התקנה ממקור זה במכשיר.
3. פתח את האפליקציה ועבור את אשף ההגדרה.
4. בצע בדיקת שיחה קצרה כדי לוודא שהכול עובד.
```

## Activation

MVP:

- 7-day full trial.
- One-time code after purchase.
- Suggested price: ₪149 for version 1.x.
- Local validation.

Do not add subscriptions in MVP.

## Update checker

Use the static version manifest defined in `context/DATA_CONTRACTS.md`.

MVP UX:

```txt
גרסה חדשה זמינה - עדכן
```

If forced:

```txt
נדרש עדכון כדי להמשיך להשתמש באפליקציה.
```

Download opens browser.

## Release checklist

- Version code incremented.
- APK/AAB release signed.
- SHA-256 recorded if used.
- `version.json` updated.
- Changelog updated.
- Landing page download link updated.
- Install tested on clean phone.

## Anti-patterns

Do not:

- distribute anonymous unsigned APKs;
- tell users to ignore all warnings without explanation;
- hide sensitive permission usage;
- add auto-update installers in MVP;
- rely on Play Billing;
- promise Play Store availability.
