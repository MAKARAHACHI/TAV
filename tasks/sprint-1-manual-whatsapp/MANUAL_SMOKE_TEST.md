# Sprint 1 Manual Smoke Test

Verification snapshot:
- Git status before manual test: uncommitted Sprint 1 files are present (`app/`, Gradle scaffold, and `tasks/` artifacts).
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` exists.
- Final human phone smoke result: PASS on a real Android phone after the reset-button fix.

Human/device checklist:
- [x] App opens.
- [x] Hebrew RTL layout is correct.
- [x] Phone field works.
- [x] Template selection works.
- [x] Message editing works.
- [x] `wa.me` preview updates.
- [x] Tap `Open WhatsApp` (`פתח WhatsApp`) opens WhatsApp or browser.
- [x] Reset message button (`איפוס הודעה`) works correctly.
- [x] No auto-send happens; user must press Send manually inside WhatsApp.

Reset fix note:
- [x] Re-test reset message button on phone after the 2026-05-17 fix.
- Code fix applied on 2026-05-17. Reset now restores the editable message from the currently selected template default through the same helper used by template selection.
- Real-phone smoke test repeated on 2026-05-17 and passed.

Stop condition:
- [x] Do not start Sprint 2 from this checklist.
