package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AccessibilityBackNavigationTest {
    @Test
    fun systemBackFromMessageEditorReturnsHome() {
        val next = AccessibilityBackNavigation.handleBack(
            AccessibilityBackState(
                tab = AccessibilityTab.HOME,
                modal = AccessibilityModal.NONE,
                homeMessageEditorOpen = true
            )
        )

        assertEquals(AccessibilityTab.HOME, next?.tab)
        assertEquals(AccessibilityModal.NONE, next?.modal)
        assertFalse(next?.homeMessageEditorOpen ?: true)
    }

    @Test
    fun systemBackFromExclusionsReturnsSettings() {
        val next = AccessibilityBackNavigation.handleBack(
            AccessibilityBackState(
                tab = AccessibilityTab.SETTINGS,
                modal = AccessibilityModal.EXCLUSIONS,
                homeMessageEditorOpen = false
            )
        )

        assertEquals(AccessibilityTab.SETTINGS, next?.tab)
        assertEquals(AccessibilityModal.NONE, next?.modal)
    }

    @Test
    fun systemBackFromRootHomeExitsNormally() {
        val next = AccessibilityBackNavigation.handleBack(
            AccessibilityBackState(
                tab = AccessibilityTab.HOME,
                modal = AccessibilityModal.NONE,
                homeMessageEditorOpen = false
            )
        )

        assertNull(next)
    }
}
