package com.cybervictim.assistant.ai

import java.util.concurrent.atomic.AtomicReference

/**
 * Holds the active case session (Phase 2 will persist this in Room).
 * On-device only per safety rules.
 */
class CaseSessionStore {
    val caseFacts = StringBuilder()
    fun onUserMessage(text: String) {
        caseFacts.append('\n').append(text.take(2000))
    }

    companion object {
        @Volatile var current: CaseSessionStore? = null
    }
}
