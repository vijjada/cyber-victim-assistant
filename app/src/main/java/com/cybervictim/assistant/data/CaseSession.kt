package com.cybervictim.assistant.data

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.launch

/** One active case at a time; created when the Guide starts a session. */
object CaseSession {
    private val active = AtomicReference<String?>(null)
    @Volatile var dbRef: AtomicReference<CaseDb>? = null // wired by MainActivity

    fun ensure(): String =
        active.get() ?: ("CASE-" + System.currentTimeMillis().toString(36)).also {
            active.set(it)
            dbRef?.get()?.let { db ->
                CaseDb.scope.launch {
                    db.dao().addEvent(
                        CaseEvent(caseId = it, kind = "created", detail = "Case session started by AI Case Guide")
                    )
                }
            }
        }

    fun id(): String? = active.get()
}
