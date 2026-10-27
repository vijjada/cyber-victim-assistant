package com.cybervictim.assistant.data

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.launch

/** One active case at a time; created when the Guide starts a session. */
object CaseSession {
    private val active = AtomicReference<String?>(null)
    @Volatile var dbRef: AtomicReference<CaseDb>? = null // wired by MainActivity

    fun ensure(): String {
        val current = active.get()
        if (current != null) return current
        val newId = "CASE-" + System.currentTimeMillis().toString(36)
        if (active.compareAndSet(null, newId)) {
            dbRef?.get()?.let { db ->
                CaseDb.scope.launch {
                    db.dao().addEvent(
                        CaseEvent(caseId = newId, kind = "created", detail = "Case session started by AI Case Guide")
                    )
                }
            }
            return newId
        }
        return active.get() ?: newId
    }

    fun id(): String? = active.get()
}
