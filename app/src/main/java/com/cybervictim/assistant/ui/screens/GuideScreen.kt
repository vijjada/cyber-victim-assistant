package com.cybervictim.assistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cybervictim.assistant.ai.GeminiClient
import com.cybervictim.assistant.ai.CaseSessionStore
import kotlinx.coroutines.launch

private data class Msg(val fromUser: Boolean, val text: String)

@Composable
fun GuideScreen(nav: androidx.navigation.NavHostController) {
    val store = remember { CaseSessionStore.current }
    val gctx = androidx.compose.ui.platform.LocalContext.current
    val messages = remember { mutableStateListOf(Msg(false, com.cybervictim.assistant.ui.L10n.t("gd_welcome"))) }
    val input = remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { m ->
                Card { Text(m.text, Modifier.padding(10.dp)) }
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(Modifier.padding(8.dp)) {
            TextField(
                value = input.value,
                onValueChange = { input.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(com.cybervictim.assistant.ui.L10n.t("gd_hint")) },
                minLines = 1, maxLines = 4,
            )
            Button(
                onClick = {
                    val q = input.value.trim()
                    if (q.isEmpty()) return@Button
                    messages.add(Msg(true, q)); input.value = ""
                    busy = true
                    val gid = com.cybervictim.assistant.data.CaseSession.ensure()
                    val gdb = com.cybervictim.assistant.data.CaseDb.get(gctx)
                    com.cybervictim.assistant.data.CaseDb.scope.launch {
                        gdb.dao().addEvent(com.cybervictim.assistant.data.CaseEvent(caseId = gid, kind = "guide_chat", detail = q.take(120)))
                    }
                    // Chat runs on a background thread; UI updates via callback
                    GeminiClient.chat(
                        history = messages.map { it.fromUser to it.text },
                        userMessage = q,
                        onReply = { reply ->
                            messages.add(Msg(false, reply))
                            store?.onUserMessage(q)
                            busy = false
                        },
                        onError = { e ->
                            messages.add(Msg(false, java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("gd_err"), e)))
                            busy = false
                        },
                    )
                },
                enabled = !busy,
            ) { Text(com.cybervictim.assistant.ui.L10n.t("gd_send")) }
        }
    }
}
