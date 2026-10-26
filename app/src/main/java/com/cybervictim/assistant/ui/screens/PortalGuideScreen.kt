package com.cybervictim.assistant.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cybervictim.assistant.portal.PortalDriver
import com.cybervictim.assistant.portal.StepOwner

@Composable
fun PortalGuideScreen(nav: androidx.navigation.NavHostController) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val steps = remember { mutableStateListOf<PortalDriver.PortalStep>() }
    var busy by remember { mutableStateOf(false) }
    var verifiedDate by remember { mutableStateOf(PortalDriver.verifiedDate()) }
    var hatchOn by remember { mutableStateOf(false) }

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
                    text = com.cybervictim.assistant.ui.L10n.t("pg_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("pg_sub"), verifiedDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        AssistChip(
            onClick = {},
            label = { Text(com.cybervictim.assistant.ui.L10n.t("pg_lock")) },
        )
        AssistChip(
            onClick = {},
            label = { Text(com.cybervictim.assistant.ui.L10n.t("pg_ev")) },
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(steps) { s ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (s.owner) {
                            StepOwner.VICTIM_LOCK -> MaterialTheme.colorScheme.errorContainer
                            StepOwner.HUMAN -> MaterialTheme.colorScheme.secondaryContainer
                            StepOwner.AI_AUTO -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Text(
                        "[" + when (s.owner) {
                            StepOwner.AI_AUTO -> com.cybervictim.assistant.ui.L10n.t("pg_owner_ai")
                            StepOwner.HUMAN -> com.cybervictim.assistant.ui.L10n.t("pg_owner_you")
                            StepOwner.VICTIM_LOCK -> com.cybervictim.assistant.ui.L10n.t("pg_owner_tap")
                        } + "] " + s.narration,
                        Modifier.padding(10.dp),
                    )
                }
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    busy = true
                    val caseText = (com.cybervictim.assistant.ai.CaseSessionStore.current?.caseFacts?.toString() ?: "")
                    PortalDriver.startCaseFlowFromEvidence(
                        caseText = caseText,
                        assetsReader = { path -> ctx.assets.open(path).use { st -> st.bufferedReader().readText() } },
                    ) { batch ->
                        if (batch.isEmpty()) steps.add(PortalDriver.PortalStep(StepOwner.AI_AUTO, com.cybervictim.assistant.ui.L10n.t("pg_empty")))
                        else steps.addAll(batch)
                        busy = false
                    }
                },
                enabled = !busy,
            ) { Text(com.cybervictim.assistant.ui.L10n.t("pg_start")) }
            TextButton(onClick = { hatchOn = !hatchOn }) { Text(com.cybervictim.assistant.ui.L10n.t("pg_hatch")) }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { nav.navigate(com.cybervictim.assistant.ui.Routes.PORTAL_WEBVIEW) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
        ) {
            Text("Auto-Fill Portal (Auto-fills the form for you)")
        }
        if (hatchOn) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Text(
                    com.cybervictim.assistant.ui.L10n.t("pg_hatch_body"),
                    Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
