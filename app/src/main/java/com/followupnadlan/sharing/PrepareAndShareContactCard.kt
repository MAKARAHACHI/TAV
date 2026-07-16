package com.followupnadlan.sharing

import android.content.Context
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.missedcall.WhatsAppPackageResolver
import com.followupnadlan.profile.ContactCard
import com.followupnadlan.profile.ContactCardFileWriter
import com.followupnadlan.profile.MyDetailsStore
import com.followupnadlan.profile.VCardBuilder

/**
 * Use case: load saved details → validate → build vCard → write temp file → launch share, then
 * log only after a successful open. The UI just maps the returned [ContactCardShareResult]; it
 * knows nothing about files, Uris, or Intents. The card reuses the existing [MyDetailsStore]
 * (agent name / office / phone) so there is a single source of truth for those fields.
 */
class PrepareAndShareContactCard(
    private val context: Context,
    private val profileStore: MyDetailsStore = MyDetailsStore(context.applicationContext),
    private val fileWriter: ContactCardFileWriter = ContactCardFileWriter(context.applicationContext),
    private val launcher: ContactCardShareLauncher = ContactCardShareLauncher(context)
) {
    operator fun invoke(preferredPackage: String): ContactCardShareResult {
        val card = ContactCard.fromProfile(profileStore.load())
        val vCard = VCardBuilder.build(card) ?: return ContactCardShareResult.IncompleteProfile

        val uri = try {
            fileWriter.write(vCard)
        } catch (_: Exception) {
            return ContactCardShareResult.Failed("לא הצלחנו להכין את כרטיס איש הקשר. נסה/י שוב.")
        }

        return when (val result = launcher.launch(uri, preferredPackage)) {
            is ContactCardShareLauncher.LaunchResult.Opened -> {
                logOpened(preferredPackage, result.targetOpening)
                ContactCardShareResult.Opened(preferredPackage, result.targetOpening)
            }
            ContactCardShareLauncher.LaunchResult.NoReceiver ->
                ContactCardShareResult.Failed("לא נמצאה אפליקציה שיכולה לשתף כרטיס איש קשר.")
        }
    }

    /** Non-identifying metadata only — never the phone, name, org, vCard body, Uri, or file path. */
    private fun logOpened(preferredPackage: String, targetOpening: TargetOpening) {
        val packageLabel = when (preferredPackage) {
            WhatsAppPackageResolver.WHATSAPP_BUSINESS_PACKAGE -> "whatsapp_business"
            WhatsAppPackageResolver.WHATSAPP_MESSENGER_PACKAGE -> "whatsapp"
            else -> "chooser"
        }
        val openingLabel = when (targetOpening) {
            TargetOpening.DIRECT -> "direct"
            TargetOpening.FALLBACK_CHOOSER -> "fallback_chooser"
        }
        FollowUpLogStore(context.applicationContext).append(
            FollowUpLogEntry(
                actionType = FollowUpActionType.CONTACT_CARD_OPENED,
                timestampEpochMs = System.currentTimeMillis(),
                messagePreview = "$packageLabel/$openingLabel",
                phone = "",
                source = MissedCallAutoResponseSettings.SOURCE
            )
        )
    }
}
