# Manual QA Checklist: Sprint 17 Beta Candidate

## Device

- Android version:
- Manufacturer:
- Model:
- SIM active: yes/no
- WhatsApp installed: yes/no
- WhatsApp Business installed: yes/no
- Accessibility enabled: yes/no
- SMS permission granted: yes/no

## A. WhatsApp Manual Mode

- Enable missed-call response.
- Choose WhatsApp first.
- Disable WhatsApp auto-send.
- Miss an incoming call.
- Verify WhatsApp opens with correct number and message.
- Verify SMS is not sent.
- Verify log says prepared/opened, not sent.

Result:

Notes:

## B. WhatsApp Accessibility Auto Mode

- Enable Accessibility service.
- Enable WhatsApp auto-send.
- Miss an incoming call.
- Verify WhatsApp opens.
- Verify message sends only if send button is correctly detected.
- Verify log says sent only after click.
- Verify no duplicate SMS.

Result:

Notes:

## C. SMS Fallback

- Disable/uninstall WhatsApp if possible, or simulate WhatsApp open failure.
- Miss incoming call.
- Verify SMS fallback.
- Verify SMS permission behavior.

Result:

Notes:

## D. Permission Denied

- Deny SMS permission.
- Disable Accessibility.
- Miss incoming call.
- Verify safe fallback / notification.
- Verify no silent failure.

Result:

Notes:

## E. Duplicate Protection

- Miss call from same number twice within 6 hours.
- Verify only one response.

Result:

Notes:

## F. Private/Unknown Number

- Verify no response is sent.

Result:

Notes:

## G. OEM/Background Behavior

- Test after screen locked.
- Test after app swiped away if possible.
- Test battery optimization effects.

Result:

Notes:

## Final Manual QA Decision

- Pass / Pass with notes / Fail:
- Blocking issues:
- Device evidence owner:
- Date:
