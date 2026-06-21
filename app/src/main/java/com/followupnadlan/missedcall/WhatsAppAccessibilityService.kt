package com.followupnadlan.missedcall

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WhatsAppAccessibilityService : AccessibilityService() {
    private val controller by lazy { WhatsAppAutoSendController(applicationContext) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString().orEmpty()
        if (packageName != WhatsAppPackageResolver.WHATSAPP_MESSENGER_PACKAGE &&
            packageName != WhatsAppPackageResolver.WHATSAPP_BUSINESS_PACKAGE
        ) {
            return
        }

        val now = System.currentTimeMillis()
        val pending = controller.readPending(now) ?: return
        if (pending.packageName != packageName) return

        val root = rootInActiveWindow ?: run {
            if (shouldFailPending(pending, now)) {
                controller.markFailed(pending, now)
            }
            return
        }

        val sendButton = findSendButton(root, packageName)
        if (sendButton != null && sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            controller.markSent(pending, now)
        } else if (sendButton != null) {
            controller.markFailed(pending, now)
        } else if (shouldFailPending(pending, now)) {
            controller.markFailed(pending, now)
        }
    }

    override fun onInterrupt() = Unit

    private fun findSendButton(root: AccessibilityNodeInfo, packageName: String): AccessibilityNodeInfo? {
        val byId = root.findAccessibilityNodeInfosByViewId("$packageName:id/send")
            .firstNotNullOfOrNull(::clickableTarget)
        if (byId != null) return byId

        return findClickableSendNode(root)
    }

    private fun findClickableSendNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (isSendLabel(node)) {
            val clickableNode = clickableTarget(node)
            if (clickableNode != null) return clickableNode
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val result = findClickableSendNode(child)
            if (result != null) return result
        }
        return null
    }

    private fun isSendLabel(node: AccessibilityNodeInfo): Boolean {
        val labels = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            .map(::normalizeLabel)
        return labels.any {
            it == "send" ||
                it == "\u05E9\u05DC\u05D7" ||
                it == "\u05E9\u05DC\u05D9\u05D7\u05D4"
        }
    }

    private fun clickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isVisibleToUser && current.isEnabled && current.isClickable) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun normalizeLabel(label: String): String =
        label.trim().lowercase().replace("\u200f", "").replace("\u200e", "")

    private fun shouldFailPending(pending: PendingWhatsAppAutoSend, nowEpochMs: Long): Boolean =
        WhatsAppAutoSendAttemptDecider.whenSendButtonMissing(
            pendingCreatedAtEpochMs = pending.createdAtEpochMs,
            nowEpochMs = nowEpochMs
        ) == WhatsAppAutoSendAttemptOutcome.FAIL_PENDING
}
