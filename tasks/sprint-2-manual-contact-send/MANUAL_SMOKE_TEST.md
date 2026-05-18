# Sprint 2B Manual Smoke Test

Verification snapshot:
- Sprint 2B source/build validation: `.\gradlew.bat test assembleDebug` passed on 2026-05-19 after rerunning outside the sandbox for Gradle wrapper lock access.
- Final human phone smoke result: PASS on a real Android phone, reported by the user on 2026-05-19.

Human/device checklist:
- [x] App opens on a real Android phone.
- [ ] Hebrew RTL layout remains readable.
- [x] The manual composer shows visible actions: `פתח WhatsApp`, `שיתוף ידני`, and `העתק הודעה`.
- [x] Valid WhatsApp number opens WhatsApp through `פתח WhatsApp` with the prepared message.
- [x] User must still press Send manually inside WhatsApp.
- [ ] A number that does not exist in WhatsApp may show a WhatsApp-side error.
- [ ] After a WhatsApp-side invalid-number error, returning to the app still shows `שיתוף ידני` and `העתק הודעה`.
- [x] `שיתוף ידני` opens the Android Sharesheet / chooser.
- [x] `שיתוף ידני` shares only the prepared message text.
- [x] `שיתוף ידני` does not require a valid WhatsApp number.
- [x] `העתק הודעה` copies the exact prepared message text.
- [x] Copied text can be pasted into another app and matches the composed message.
- [x] Copy action shows a clear confirmation message.
- [ ] Empty message shows clear validation for WhatsApp, manual share, or copy.
- [ ] Reset message still restores the currently selected template message.
- [ ] Reset message does not clear the phone/contact field.

Forbidden behavior checklist:
- [ ] No auto-send happens.
- [ ] No WhatsApp-number existence detection happens.
- [ ] No Accessibility Service behavior exists.
- [ ] No contacts permission prompt appears.
- [ ] No SMS permission prompt appears.
- [ ] No new Android permission prompt appears.
- [ ] No CRM, backend/API, Snooze, post-call, scheduler, or reminder behavior appears in Sprint 2B.

Stop condition:
- [x] Sprint 2B manual smoke was tested on a real Android phone before recording PASS.
