# Data Contracts

This MVP has no backend API.

Use this file for local app models, Room persistence contracts, template structure, follow-up task structure, activation license state, and the static version update JSON manifest.

Do not introduce REST endpoints, backend servers, cloud sync, or third-party API integrations in MVP.

## Phone number format

Store normalized phone numbers in international format without `+` when used for WhatsApp:

```txt
972501234567
```

Input may be:

```txt
050-1234567
+972501234567
972501234567
```

Normalization rules:

- Remove spaces, hyphens, parentheses.
- If Israeli local mobile starts with `0`, convert to `972` + number without leading zero.
- Preserve international numbers where possible.
- Validate enough for wa.me; do not over-block unusual valid numbers.

## WhatsApp open contract

Input:

```kotlin
phone: String // normalized international number without plus
message: String // final rendered text
preferBusiness: Boolean? // optional later
```

Output:

- Launch `ACTION_VIEW` with `https://wa.me/<phone>?text=<encoded>`.
- If both WhatsApp and WhatsApp Business exist, default can be system chooser unless settings specify one.
- If no handler exists, show a clear Hebrew error.

## Local storage ownership

- Room stores durable app data: agent profile, leads, templates, message blocks, follow-up tasks, and message draft/history if later approved.
- DataStore or SharedPreferences stores small app settings and onboarding flags.
- The update check reads one static JSON file from the distribution website. That file is not an app backend API.
- Activation is validated locally in MVP and then stored locally.

## AgentProfile model

```kotlin
data class AgentProfile(
  val id: Long = 1,
  val fullName: String,
  val businessName: String?,
  val phone: String?,
  val website: String?,
  val signature: String,
  val createdAt: Instant,
  val updatedAt: Instant
)
```

Room entity:

```kotlin
@Entity(tableName = "agent_profile")
data class AgentProfileEntity(
  @PrimaryKey val id: Long = 1,
  val fullName: String,
  val businessName: String?,
  val phone: String?,
  val website: String?,
  val signature: String,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long
)
```

## Lead model

```kotlin
data class Lead(
  val id: Long,
  val fullName: String?,
  val phone: String,
  val type: LeadType?,
  val status: LeadStatus,
  val notes: String?,
  val lastCallAt: Instant?,
  val lastFollowUpAt: Instant?,
  val createdAt: Instant,
  val updatedAt: Instant
)
```

Room entity:

```kotlin
@Entity(
  tableName = "leads",
  indices = [Index("phone")]
)
data class LeadEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val fullName: String?,
  val phone: String,
  val type: String?,
  val status: String,
  val notes: String?,
  val lastCallAtEpochMs: Long?,
  val lastFollowUpAtEpochMs: Long?,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long
)
```

Enums:

```kotlin
enum class LeadType {
  BUYER, SELLER, INVESTOR, RENTER, LANDLORD, PROJECT_LEAD, UNKNOWN
}

enum class LeadStatus {
  NEW, FOLLOW_UP_NEEDED, MEETING_SET, SENT_DETAILS, NOT_RELEVANT, CLOSED
}
```

## Template schema

```kotlin
data class Template(
  val id: String,
  val title: String,
  val category: TemplateCategory,
  val body: String,
  val isDefault: Boolean,
  val isBuiltIn: Boolean,
  val createdAt: Instant,
  val updatedAt: Instant
)
```

Room entity:

```kotlin
@Entity(tableName = "templates")
data class TemplateEntity(
  @PrimaryKey val id: String,
  val title: String,
  val category: String,
  val body: String,
  val isDefault: Boolean,
  val isBuiltIn: Boolean,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long
)
```

Categories:

```kotlin
enum class TemplateCategory {
  BUYER,
  SELLER,
  INVESTOR,
  MISSED_CALL,
  AFTER_MEETING,
  GENERAL
}
```

## Template placeholders

Supported MVP placeholders:

```txt
{lead_name}
{phone}
{agent_name}
{business_name}
{agent_phone}
{website}
{signature}
{city}
{neighborhood}
{address}
{rooms}
{price}
{floor}
{parking}
{balcony}
{property_link}
{suggested_time}
```

Unresolved placeholders should be handled visibly but safely:

- Preferred MVP behavior: leave them highlighted/editable in the composer.
- Do not send the user to WhatsApp with raw placeholders unless user confirms.

## MessageBlock schema

```kotlin
data class MessageBlock(
  val id: String,
  val title: String,
  val group: BlockGroup,
  val text: String,
  val sortOrder: Int,
  val isBuiltIn: Boolean
)
```

Room entity:

```kotlin
@Entity(tableName = "message_blocks")
data class MessageBlockEntity(
  @PrimaryKey val id: String,
  val title: String,
  val group: String,
  val text: String,
  val sortOrder: Int,
  val isBuiltIn: Boolean
)
```

Block groups:

```kotlin
enum class BlockGroup {
  OPENING,
  CONTEXT,
  PROPERTY_DETAILS,
  CALL_TO_ACTION,
  CLOSING,
  SIGNATURE
}
```

## FollowUpTask schema

This is the central contract for post-call and snooze.

```kotlin
data class FollowUpTask(
  val id: Long,
  val phone: String?,
  val contactName: String?,
  val callEndedAt: Instant?,
  val callDurationSeconds: Long?,
  val source: FollowUpSource,
  val selectedTemplateId: String?,
  val draftText: String?,
  val leadType: LeadType?,
  val propertyLink: String?,
  val reminderAt: Instant?,
  val status: FollowUpStatus,
  val createdAt: Instant,
  val updatedAt: Instant
)
```

Room entity:

```kotlin
@Entity(
  tableName = "follow_up_tasks",
  indices = [Index("status"), Index("reminderAtEpochMs")]
)
data class FollowUpTaskEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val phone: String?,
  val contactName: String?,
  val callEndedAtEpochMs: Long?,
  val callDurationSeconds: Long?,
  val source: String,
  val selectedTemplateId: String?,
  val draftText: String?,
  val leadType: String?,
  val propertyLink: String?,
  val reminderAtEpochMs: Long?,
  val status: String,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long
)
```

Behavior notes:

- `FollowUpTask` is the central local object for post-call and snooze flows.
- Create it locally after a detected call end or manual follow-up entry.
- Update it locally when the user opens WhatsApp, snoozes, dismisses, or saves as lead.
- Do not sync it to any server in MVP.

Enums:

```kotlin
enum class FollowUpSource {
  POST_CALL_AUTO,
  MANUAL,
  SNOOZE_REMINDER,
  MISSED_CALL
}

enum class FollowUpStatus {
  DRAFT,
  OPENED,
  SNOOZED,
  WHATSAPP_OPENED,
  SAVED_AS_LEAD,
  DISMISSED
}
```

## Notification contract

Channels:

```txt
post_call_followup
snooze_reminders
service_status
updates
```

Post-call notification actions:

```txt
OPEN_CARD
SNOOZE_DEFAULT
DISMISS
```

Snooze notification actions:

```txt
OPEN_CARD
SNOOZE_AGAIN
DISMISS
```

## Snooze contract

MVP presets:

```txt
15 minutes
30 minutes
1 hour
evening default 20:00 local time
tomorrow morning default 09:00 local time
```

Use WorkManager for MVP. Do not promise exact-to-the-minute reminders.

## Activation license schema

Store activation state locally after trial start and local code validation.

```kotlin
data class ActivationLicenseState(
  val installedAt: Instant,
  val trialEndsAt: Instant,
  val activated: Boolean,
  val activationCodeHash: String?,
  val activatedAt: Instant?
)
```

Room entity is optional in MVP. Local preference storage is acceptable because this is a single-device app state.

Rules:

- MVP uses a 7-day local trial.
- Activation validation is local-only in MVP.
- Internet is not required after activation succeeds.
- Do not add online license checks, device binding, revocation, or account linkage in MVP.

## Version update JSON schema

Static manifest location:

```txt
https://followup-nadlan.co.il/version.json
```

This is a static website file used by the APK updater flow. It is not a backend API surface for the app.

Shape:

```json
{
  "latest": "1.2.0",
  "minSupported": "1.0.0",
  "url": "https://followup-nadlan.co.il/download/app-v1.2.0.apk",
  "force": false,
  "notesHe": "תיקוני יציבות ושיפור תזכורות",
  "sha256": "optional-sha256"
}
```

Behavior:

- If `latest` > current and `force=false`: show update banner.
- If `force=true` or current < `minSupported`: block normal use until update.
- Download opens the browser URL in MVP.

## Error conventions

Use simple Hebrew error copy.

Examples:

```txt
לא הצלחנו לפתוח את WhatsApp. בדוק שהאפליקציה מותקנת.
לא נמצאה הרשאת יומן שיחות. אפשר להמשיך במצב ידני.
התזכורת נשמרה, אבל ייתכן שהמכשיר יעכב אותה בגלל חיסכון בסוללה.
```
