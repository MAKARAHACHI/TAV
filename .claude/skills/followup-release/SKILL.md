---
name: followup-release
description: FollowUp release + OTP ops. Use when shipping an app update (bump→build→distribute via Firebase), managing OTP users (revoke/list), checking the OTP or WhatsApp-bot server, or reconnecting to any of this infra. Run /followup-release.
---

# FollowUp — Release & OTP Operations

Everything needed to ship an update and manage users, without relearning it each time.
The app is Hebrew RTL, sideloaded via Firebase App Distribution (NOT Play — Play Protect blocks it,
that's expected). Work happens in the git worktree **`F:\followup`** (`/f/followup`); prefix every
Bash command with `cd /f/followup &&` (shell cwd resets between calls).

## Secrets — NOT in this file (it's committed to git)
- **ADMIN_KEY** (revokes OTP users): the user has it saved. Ask them, or read from their store. Never write it here.
- **keystore passwords**: in `F:\followup\keystore.properties` (gitignored). The signing keystore is
  `F:\followup\keystore\followup-release.jks` (gitignored) — losing it = no more updates ever. It must be backed up.

## Key facts (verified 2026-07-31)
- Firebase project `followup-app-il`; Android App ID `1:711103587669:android:f8a2768d6c5b7faa1c3bdc`; package `com.followupnadlan`.
- OTP server base URL (public, no api-key): **`https://otpf.ta-v.com`** (note the "otpf" — real spelling).
- OTP sender WhatsApp number: `+972542266890`.
- Infra VM: `whatsapp-bot-vm` zone `us-central1-a`, via `gcloud compute ssh ... --command="..."` (each call is a fresh SSH session; docker needs `sudo`). Containers: `baileys-api-otp-server-1` (our OTP server, port 3100), `baileys-api-app-1` (baileys, 3000→3025), `baileys-api-redis-1`, `cloudflared-tunnel`. See also skill `wa-infra` for baileys internals.
- Firebase CLI must be logged in as **danielayalo4@gmail.com** (`firebase login:list`).

---

## TASK A — Ship an app update (the common one)

1. **Bump version** in `app/build.gradle.kts` — `versionCode` MUST increase (Android refuses same-code updates); bump `versionName` too. E.g. 2/"0.1.1" → 3/"0.1.2".
2. **Build signed release**:
   ```
   cd /f/followup && ./gradlew :app:assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`. (Verify signature CN=FollowUp if unsure via apksigner.)
3. **Distribute** (all pilot testers get an App-Tester notification):
   ```
   firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk \
     --app 1:711103587669:android:f8a2768d6c5b7faa1c3bdc \
     --project followup-app-il \
     --release-notes "<what changed>" \
     --groups "pilot"
   ```
4. **Commit** the version bump + code (never commit keystore/keystore.properties — they're gitignored). Push.
- New tester: add their email in Firebase Console → App Distribution → Testers, or `firebase appdistribution:testers:add "<email>" --project followup-app-il`. They install "App Tester" once, then FollowUp. They will see a Play-Protect "האפליקציה נחסמה" screen (sideload + accessibility/CALL_LOG perms — unavoidable); they need the install guide to get past it.

## TASK B — Manage OTP users (revoke / list) — remote control

Needs ADMIN_KEY (ask user; do not hardcode). Base `https://otpf.ta-v.com`.
- **Revoke a user** (locks their app within ≤7 days, or immediately on their next online launch):
  ```
  curl -s -X POST https://otpf.ta-v.com/otp/admin/revoke \
    -H "x-admin-key: $ADMIN_KEY" -H "content-type: application/json" \
    -d '{"phone":"05XXXXXXXX"}'
  ```
  Fallback on the VM: `redis-cli HSET user:<972digits> revoked 1`.
- **List users**:
  ```
  curl -s -X POST https://otpf.ta-v.com/otp/admin/list -H "x-admin-key: $ADMIN_KEY"
  ```
- **Un-revoke**: on the VM, `redis-cli HSET user:<972digits> revoked 0` (they re-login next launch).

## TASK C — Check / reconnect the OTP server

- Health (external): `curl -s https://otpf.ta-v.com/health` → `{"ok":true}`.
- Containers: `gcloud compute ssh whatsapp-bot-vm --zone=us-central1-a --command="sudo docker ps" --quiet`
- Restart ONLY the OTP server (never the others): `... --command="cd ~/baileys-api && sudo docker compose up -d otp-server"`
- Logs: `... --command="sudo docker logs baileys-api-otp-server-1 --tail 50"`
- Server code lives in `~/otp-server/` on the VM. Endpoints: `/otp/request`, `/otp/verify`, `/otp/renew`, `/otp/admin/*`. Do NOT touch baileys source; only its docker-compose has our added `otp-server` service.
- **Do NOT** "Refresh token" or reinstall the Cloudflare tunnel — it kills the live tunnel (wa + otp). The tunnel is dashboard-managed; the OTP public hostname `otpf.ta-v.com` → `http://localhost:3100` (cloudflared runs host-network, so it's localhost, not the docker name).

## TASK D — Update-broadcast to all users (data exists, feature not built yet)
The server has `user:*` records + a live WhatsApp connection, so a broadcast = loop `/otp/admin/list`
phones and send via baileys send-message. Not implemented; note it if the user asks to notify everyone.

## Guardrails
- Never commit secrets (keystore, keystore.properties, ADMIN_KEY). They're gitignored — keep it that way.
- Never bump-less distribute (same versionCode won't install).
- Never restart baileys/redis/cloudflared to fix the OTP server — only `otp-server`.
- Full background: memory `followup-otp-login-gate` + plan `C:\Users\danie\.claude\plans\lovely-pondering-blanket.md`.
