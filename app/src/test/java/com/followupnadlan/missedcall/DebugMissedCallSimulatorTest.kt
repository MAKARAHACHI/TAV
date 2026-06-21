package com.followupnadlan.missedcall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DebugMissedCallSimulatorTest {
    @Test
    fun simulatorDelegatesSameMissedCallCandidateShapeToHandlerCallback() {
        var capturedCandidate: MissedCallCandidate? = null
        val simulator = DebugMissedCallSimulator(isDebugBuild = true) { candidate ->
            capturedCandidate = candidate
        }

        val result = simulator.simulate("0501234567")

        assertEquals(DebugMissedCallSimulationResult.TRIGGERED, result)
        assertEquals(
            MissedCallCandidate(
                phoneNumber = "0501234567",
                direction = MissedCallDirection.INCOMING,
                wasAnswered = false,
                source = DebugMissedCallSimulator.SOURCE
            ),
            capturedCandidate
        )
    }

    @Test
    fun emptyNumberIsRejected() {
        var capturedCandidate: MissedCallCandidate? = null
        val simulator = DebugMissedCallSimulator(isDebugBuild = true) { candidate ->
            capturedCandidate = candidate
        }

        val result = simulator.simulate("   ")

        assertEquals(DebugMissedCallSimulationResult.EMPTY_NUMBER, result)
        assertNull(capturedCandidate)
    }

    @Test
    fun releaseBuildGuardBlocksSimulation() {
        var capturedCandidate: MissedCallCandidate? = null
        val simulator = DebugMissedCallSimulator(isDebugBuild = false) { candidate ->
            capturedCandidate = candidate
        }

        val result = simulator.simulate("0501234567")

        assertEquals(DebugMissedCallSimulationResult.RELEASE_BUILD_BLOCKED, result)
        assertNull(capturedCandidate)
    }

    @Test
    fun cooldownStillBlocksDuplicateDebugEvent() {
        var decision: MissedCallAutoResponseAction? = null
        val simulator = DebugMissedCallSimulator(isDebugBuild = true) { candidate ->
            decision = MissedCallAutoResponseDecision.decide(
                MissedCallAutoResponseInput(
                    direction = candidate.direction,
                    wasAnswered = candidate.wasAnswered,
                    phoneNumber = candidate.phoneNumber,
                    featureEnabled = true,
                    primaryChannel = MissedCallResponsePrimaryChannel.WHATSAPP_FIRST,
                    whatsappInstalled = true,
                    whatsappBusinessInstalled = false,
                    whatsappMode = MissedCallWhatsAppMode.PREPARED_MANUAL,
                    whatsappAutomationEnabled = false,
                    whatsappAccessibilityEnabled = false,
                    smsFallbackEnabled = true,
                    smsPermissionGranted = true,
                    lastAutoReplyAtEpochMs = 10_000L,
                    nowEpochMs = 20_000L,
                    templateAvailable = true,
                    manualFallbackAvailable = true
                )
            )
        }

        simulator.simulate("0501234567")

        assertEquals(MissedCallAutoResponseAction.SKIP_DUPLICATE, decision)
    }
}
