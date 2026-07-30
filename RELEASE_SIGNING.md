# Release Signing (FollowUp — Firebase App Distribution pilot)

This app is distributed as a **signed release APK** via Firebase App Distribution
(not Google Play). We use a **self-generated, stable keystore** so every build is
signed with the same key.

## Files (both are gitignored — NEVER commit them)

| File | Purpose | Committed? |
|------|---------|-----------|
| `keystore/followup-release.jks` | The release keystore (signing key) | NO — gitignored |
| `keystore.properties` | Holds the store/key passwords + alias | NO — gitignored |

Both paths are listed in `.gitignore` (`/keystore.properties` and `/keystore/`).
Do not remove those lines and do not `git add -f` these files.

## ⚠️ CRITICAL — BACK UP THE KEYSTORE NOW

**If you lose `keystore/followup-release.jks` (or its password), you can NEVER ship
an update to phones that already have the app installed.** Firebase App Distribution
(like Android in general) requires every update to be signed with the *same* key as
the installed version. A lost key = every pilot user must uninstall and reinstall.

Do this today:
1. Copy `keystore/followup-release.jks` to a safe offline/backup location.
2. Store the passwords in a password manager.

There is no recovery if this key is lost. It is generated with 30-year validity so it
outlives the pilot; the only real risk is losing the file.

## Current passwords are PLACEHOLDERS — rotate them

The keystore currently uses placeholder passwords and must be rotated to real secrets
before real distribution.

> **Note on PKCS12:** `keytool` created a PKCS12 keystore, which does **not** support a
> separate key password — the key password always equals the store password. So
> `keystore.properties` has `keyPassword` = `storePassword`. Keep them identical.

### Option A — change the password on the existing keystore (keeps the same key)

Preferred: it preserves the exact same signing key, so already-installed pilot builds
can still be updated.

```sh
# From the repo root (F:\followup). Replace NEWPASS with your real password.
# 1. Change the store password:
keytool -storepasswd -keystore keystore/followup-release.jks \
  -storepass CHANGEME_store -new NEWPASS

# 2. Change the key password to match (PKCS12 requires it to equal the store password):
keytool -keypasswd -keystore keystore/followup-release.jks -alias followup \
  -storepass NEWPASS -keypass CHANGEME_store -new NEWPASS
```

Then update the two password lines in `keystore.properties`:

```
storePassword=NEWPASS
keyPassword=NEWPASS
```

### Option B — generate a brand-new keystore with real passwords

Only do this if you have NOT yet distributed any build. A new keystore is a **new key**,
so it cannot update installs signed by the old key.

```sh
# From the repo root (F:\followup). Replace NEWPASS with your real password.
rm keystore/followup-release.jks
keytool -genkeypair -v \
  -keystore keystore/followup-release.jks \
  -alias followup -keyalg RSA -keysize 2048 -validity 10950 \
  -storepass NEWPASS -keypass NEWPASS \
  -dname "CN=FollowUp, OU=Pilot, O=FollowUp, L=Israel, C=IL"
```

Then set both `storePassword` and `keyPassword` in `keystore.properties` to `NEWPASS`.

## Build the signed release APK

```sh
# From the repo root (F:\followup)
./gradlew :app:assembleRelease
```

Output APK lands at:

```
app/build/outputs/apk/release/app-release.apk
```

Upload that APK to Firebase App Distribution.

### Verify the signature

```sh
# Path may vary by build-tools version:
"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Expected signer DN: `CN=FollowUp, OU=Pilot, O=FollowUp, L=Israel, C=IL`.

## How it is wired (for reference)

`app/build.gradle.kts` loads `keystore.properties` from the repo root and defines a
`release` signing config guarded by `keystorePropertiesFile.exists()`, so the project
still configures on a machine that has no keystore (the release build just won't be
signed there). The `buildTypes.release` block references that config via
`signingConfig = signingConfigs.getByName("release")`.
