package com.cybervictim.assistant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cybervictim.assistant.data.CaseDb
import com.cybervictim.assistant.data.CaseSession

/** Screen 1 — real case state driven by the on-device DB. */
@Composable
fun HomeScreen(nav: androidx.navigation.NavHostController) {
    val ctx = LocalContext.current
    val db = remember { CaseDb.get(ctx).dao() }
    val caseId = remember { CaseSession.id() }
    val events by (caseId?.let { db.eventsOf(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList<com.cybervictim.assistant.data.CaseEvent>())).collectAsState(initial = emptyList())
    val facts by (caseId?.let { db.factsOf(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList<com.cybervictim.assistant.data.CaseFact>())).collectAsState(initial = emptyList())
    val evidence by (caseId?.let { db.evidenceOf(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList<com.cybervictim.assistant.data.EvidenceItem>())).collectAsState(initial = emptyList())
    val hasCase = caseId != null && (events.isNotEmpty() || facts.isNotEmpty() || evidence.isNotEmpty())

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Button(onClick = { nav.navigate(com.cybervictim.assistant.ui.Routes.GUIDE) }, modifier = Modifier.fillMaxWidth().height(72.dp)) {
            Text(com.cybervictim.assistant.ui.L10n.t("home_help"), style = MaterialTheme.typography.titleLarge)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(com.cybervictim.assistant.ui.L10n.t("home_open_case"), style = MaterialTheme.typography.labelLarge)
                Text(if (hasCase) java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("home_case_info"), caseId, events.size) else com.cybervictim.assistant.ui.L10n.t("home_no_case"))
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(com.cybervictim.assistant.ui.L10n.t("home_next"), style = MaterialTheme.typography.labelLarge)
                Text(
                    when {
                        !hasCase -> com.cybervictim.assistant.ui.L10n.t("home_start0")
                        evidence.isEmpty() -> com.cybervictim.assistant.ui.L10n.t("home_add_evidence")
                        facts.isEmpty() -> com.cybervictim.assistant.ui.L10n.t("home_analyze")
                        else -> com.cybervictim.assistant.ui.L10n.t("home_go_portal")
                    }
                )
            }
        }
        if (hasCase && events.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(com.cybervictim.assistant.ui.L10n.t("home_latest"), style = MaterialTheme.typography.labelLarge)
                    Text(events.first().kind + ": " + events.first().detail.take(80))
                }
            }
        }
        EmergencyDialerCards()
        Spacer(Modifier.height(2.dp))
    }
}

/** Phase 6-A: one-tap dialer cards for the two national helplines (ACTION_DIAL only — no permission needed). */
@Composable
private fun EmergencyDialerCards() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    fun dial(number: String) {
        ctx.startActivity(
            android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + number))
        )
    }
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clickable { dial("1930") }.padding(16.dp)
        ) {
            Text(com.cybervictim.assistant.ui.L10n.t("dial_1930"), style = MaterialTheme.typography.titleMedium)
            Text(com.cybervictim.assistant.ui.L10n.t("dial_1930_sub"), style = MaterialTheme.typography.bodySmall)
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clickable { dial("1927") }.padding(16.dp)
        ) {
            Text(com.cybervictim.assistant.ui.L10n.t("dial_1927"), style = MaterialTheme.typography.titleMedium)
            Text(com.cybervictim.assistant.ui.L10n.t("dial_1927_sub"), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Screen 6 — real timeline of the case. */
@Composable
fun TrackerScreen(nav: androidx.navigation.NavHostController) {
    com.cybervictim.assistant.voice.TrackScreenDescriptor(
        com.cybervictim.assistant.voice.ScreenDescriptor(
            screenId = "tracker",
            title = "Case Timeline & Event Tracker",
            buttons = listOf("Back"),
            fields = emptyList(),
            owner = com.cybervictim.assistant.portal.StepOwner.AI_AUTO,
            hintKey = "voice_hint_tracker"
        )
    )
    val ctx = LocalContext.current
    val db = remember { CaseDb.get(ctx).dao() }
    val caseId = remember { CaseSession.id() }
    val events by (caseId?.let { db.eventsOf(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList<com.cybervictim.assistant.data.CaseEvent>())).collectAsState(initial = emptyList())

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Top Bar with Back Arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = com.cybervictim.assistant.ui.L10n.t("trk_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "History of events & complaint progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (events.isEmpty()) Text(com.cybervictim.assistant.ui.L10n.t("trk_empty"))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(events) { e ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(e.kind.uppercase(), style = MaterialTheme.typography.labelSmall)
                        Text(e.detail.take(140), style = MaterialTheme.typography.bodySmall)
                        Text(java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.US).format(java.util.Date(e.createdAt)),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
