package com.cybervictim.assistant.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.launch
import com.cybervictim.assistant.data.CaseSession
import com.cybervictim.assistant.data.CaseDb
import com.cybervictim.assistant.data.EvidenceItem

@Composable
fun EvidenceScreen(nav: androidx.navigation.NavHostController) {
    val ctx = LocalContext.current
    val db = remember { CaseDb.get(ctx).dao() }
    val caseId = remember { CaseSession.ensure() }
    val items by db.evidenceOf(caseId).collectAsState(initial = emptyList())
    val facts by db.factsOf(caseId).collectAsState(initial = emptyList())

        val prefsAi = ctx.getSharedPreferences("cva_settings", android.content.Context.MODE_PRIVATE)
        val aiRefine = remember { mutableStateOf(prefsAi.getBoolean("ai_refine_ocr", false)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = "evidence-" + System.currentTimeMillis().toString(36)
            val savedUri = uri.toString()
            CaseDb.scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val id = db.addEvidence(
                    EvidenceItem(
                        caseId = caseId, displayName = name, uri = savedUri,
                        kind = "screenshot", extractedFactIds = "[]",
                    )
                )
                // On-device OCR + rules (route A): image never leaves the phone
                val (ocrText, rules) = runCatching { com.cybervictim.assistant.evidence.OcrPipeline.processImage(ctx, uri) }
                    .getOrDefault("" to emptyList())
                rules.forEach { f ->
                    db.addFact(com.cybervictim.assistant.data.CaseFact(
                        caseId = caseId, type = f.type, valueText = f.value, sourceEvidenceId = id,
                    ))
                }
                db.addEvent(
                    com.cybervictim.assistant.data.CaseEvent(
                        caseId = caseId, kind = "evidence_added",
                        detail = name + " | ocr_chars=" + ocrText.length + " | facts=" + rules.size,
                    )
                )
                // Phase 9-C: opt-in Gemini pass over the OCR text (off by default).
                if (aiRefine.value && ocrText.length > 40) {
                    val raw = runCatching {
                        com.cybervictim.assistant.ai.GeminiClient.generateWithFallback(
                            "Extract structured facts only from this OCR text of a screenshot. Reply ONLY a JSON array like [{\"type\":\"upi_id\",\"value\":\"...\"}]. Allowed types: upi_id, txn_ref, amount, phone, timestamp, account_id, url. No prose.\nText:\n" + ocrText.take(3500),
                            700,
                        )
                    }.getOrNull()
                    val arr = raw?.let { r ->
                        runCatching {
                            val a = r.indexOf('[')
                            val b = r.lastIndexOf(']')
                            if (a >= 0 && b > a) org.json.JSONArray(r.substring(a, b + 1)) else null
                        }.getOrNull()
                    }
                    if (arr != null) {
                        val allowed = setOf("upi_id", "txn_ref", "amount", "phone", "timestamp", "account_id", "url")
                        var extra = 0
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            val ty = o.optString("type")
                            val v = o.optString("value")
                            if (ty in allowed && v.isNotBlank()) {
                                db.addFact(com.cybervictim.assistant.data.CaseFact(
                                    caseId = caseId, type = ty, valueText = v, sourceEvidenceId = id,
                                ))
                                extra += 1
                            }
                        }
                        db.addEvent(com.cybervictim.assistant.data.CaseEvent(
                            caseId = caseId, kind = "ai_refine", detail = name + " | extra=" + extra,
                        ))
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    text = com.cybervictim.assistant.ui.L10n.t("ev_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = com.cybervictim.assistant.ui.L10n.t("ev_sub"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        Button(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("ev_add")) }
FilterChip(
            selected = aiRefine.value,
            onClick = {
                aiRefine.value = !aiRefine.value
                prefsAi.edit().putBoolean("ai_refine_ocr", aiRefine.value).apply()
            },
            label = { Text(com.cybervictim.assistant.ui.L10n.t("ev_ai_refine") + if (aiRefine.value) " (On)" else " (Off)") },
        )
        var analyzing by remember { mutableStateOf(false) }
        Button(
            onClick = {
                analyzing = true
                val evidenceSummary = buildString {
                    appendLine("Uploaded evidence files: " + items.joinToString(", ") { it.displayName })
                    if (facts.isNotEmpty()) {
                        appendLine("Known facts so far:")
                        facts.forEach { appendLine("- ${it.type}: ${it.valueText}") }
                    }
                }
                com.cybervictim.assistant.ai.CaseExtractor.extract(
                    evidenceSummary,
                    { extractedFacts -> CaseDb.scope.launch { extractedFacts.forEach { f ->
                        db.addFact(com.cybervictim.assistant.data.CaseFact(
                            caseId = caseId, type = f.type, valueText = f.value, sourceEvidenceId = items.firstOrNull()?.id ?: 0))
                        }
                        analyzing = false
                    } },
                    { analyzing = false },
                )
            },
            enabled = items.isNotEmpty() && !analyzing,
        ) { if (analyzing) { CircularProgressIndicator() } else { Text(com.cybervictim.assistant.ui.L10n.t("ev_analyze")) } }
        Text(java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("ev_facts"), facts.size), style = MaterialTheme.typography.labelLarge)
        if (facts.isEmpty()) Text(com.cybervictim.assistant.ui.L10n.t("ev_facts_empty"))
        EvidenceList(items)
        Text(com.cybervictim.assistant.ui.L10n.t("ev_money"), style = MaterialTheme.typography.labelLarge)
        MoneyTrail(facts)
        var packMsg by remember { mutableStateOf<String?>(null) }
        Button(onClick = {
            // one write-side action per tap; result note is shown, never auto-submitted
            val f = com.cybervictim.assistant.pack.ComplaintPack.generatePdf(ctx, caseId, facts)
            val share = android.content.Intent(android.content.Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(android.content.Intent.EXTRA_STREAM,
                    androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".packs", f))
                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(android.content.Intent.createChooser(share, com.cybervictim.assistant.ui.L10n.t("ev_share_chooser")))
            packMsg = f.name
            CaseDb.scope.launch { db.addEvent(com.cybervictim.assistant.data.CaseEvent(caseId = caseId, kind = "pack_generated", detail = f.name)) }
        }, enabled = facts.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("ev_pack")) }
        packMsg?.let { Text(java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("ev_shared"), it), style = MaterialTheme.typography.labelSmall) }
        Button(onClick = { nav.navigate(com.cybervictim.assistant.ui.Routes.PORTAL_GUIDE) }, Modifier.fillMaxWidth()) {
            Text(com.cybervictim.assistant.ui.L10n.t("ev_to_guide"))
        }
    }
}

@Composable
private fun MoneyTrail(facts: List<com.cybervictim.assistant.data.CaseFact>) {
    val trail = facts.filter { it.type in listOf("upi_id", "txn_ref", "amount", "timestamp", "phone") }
    if (trail.isEmpty()) { Text(com.cybervictim.assistant.ui.L10n.t("ev_notrail")); return }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        trail.take(12).forEach { f ->
            Card(Modifier.fillMaxWidth()) {
                Text("${f.type.uppercase().replace('_', ' ')}: ${f.valueText}", Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EvidenceList(items: List<EvidenceItem>) {
    if (items.isEmpty()) { Text(com.cybervictim.assistant.ui.L10n.t("ev_no_evidence")); return }
    LazyColumn(Modifier.heightIn(max = 260.dp)) {
        items(items) { e ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(e.displayName, style = MaterialTheme.typography.labelLarge)
                    Text(e.kind + " • " + java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.US)
                        .format(java.util.Date(e.createdAt)), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
