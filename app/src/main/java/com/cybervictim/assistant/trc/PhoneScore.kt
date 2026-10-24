package com.cybervictim.assistant.trc

/**
 * Pure (JVM-testable) scoring core for PhoneTrace risk flags.
 * Weights mirror the manual; capped at 100.
 */
object PhoneScore {
    const val REMOTE_CONTROL = 90
    const val ACCESSIBILITY = 80
    const val SMS_READ = 60
    const val OVERLAY = 40
    const val RECENT = 30
    const val SIDELOADED = 20

    fun score(flags: List<String>): Int = flags.sumOf { f ->
        when (f) {
            "remote-control-tool" -> REMOTE_CONTROL
            "accessibility" -> ACCESSIBILITY
            "sms-read" -> SMS_READ
            "overlay" -> OVERLAY
            "recent" -> RECENT
            "installed-near-fraud-time" -> RECENT
            "sideloaded" -> SIDELOADED
            else -> 0
        }
    }.coerceAtMost(100)
}
