package com.cybervictim.assistant.trc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 6-D: PhoneTrace scoring weights (pure JVM). */
class PhoneScoreTest {

    @Test
    fun remoteControlAloneNearMax() {
        assertEquals(90, PhoneScore.score(listOf("remote-control-tool")))
    }

    @Test
    fun accessibilityPlusSmsRead() {
        assertEquals(140.coerceAtMost(100), PhoneScore.score(listOf("accessibility", "sms-read")))
    }

    @Test
    fun overlayOnly() {
        assertEquals(40, PhoneScore.score(listOf("overlay")))
    }

    @Test
    fun nearFraudTimeFlagIsWeightedRecent() {
        assertEquals(30, PhoneScore.score(listOf("installed-near-fraud-time")))
    }

    @Test
    fun unknownFlagsScoreZero() {
        assertEquals(0, PhoneScore.score(listOf("camera", "bluetooth")))
    }

    @Test
    fun everythingCapsAt100() {
        val all = listOf(
            "remote-control-tool", "accessibility", "sms-read",
            "overlay", "installed-near-fraud-time", "sideloaded",
        )
        assertEquals(100, PhoneScore.score(all))
        assertTrue(all.sumOf { it.length } > 0)
    }
}
