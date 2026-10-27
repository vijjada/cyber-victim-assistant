package com.cybervictim.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RequestsScreen(nav: androidx.navigation.NavHostController) {
    com.cybervictim.assistant.voice.TrackScreenDescriptor(
        com.cybervictim.assistant.voice.ScreenDescriptor(
            screenId = "requests",
            title = "Emergency Bank Requests",
            buttons = listOf("Bank Freeze Request", "Bank Unfreeze Request", "Police Follow-up", "Documents Request"),
            fields = emptyList(),
            owner = com.cybervictim.assistant.portal.StepOwner.AI_AUTO,
            hintKey = "voice_hint_requests"
        )
    )
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = com.cybervictim.assistant.ui.L10n.t("rq_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = com.cybervictim.assistant.ui.L10n.t("rq_sub"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val db = remember { com.cybervictim.assistant.data.CaseDb.get(ctx).dao() }
        val caseId = remember { com.cybervictim.assistant.data.CaseSession.ensure() }
        val facts by db.factsOf(caseId).collectAsState(initial = emptyList())
        var draft by remember { mutableStateOf<String?>(null) }
        Button(onClick = { draft = com.cybervictim.assistant.requests.RequestDrafter.draft("freeze", caseId, facts) }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("rq_freeze")) }
        Button(onClick = { draft = com.cybervictim.assistant.requests.RequestDrafter.draft("unfreeze", caseId, facts) }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("rq_unfreeze")) }
        Button(onClick = { draft = com.cybervictim.assistant.requests.RequestDrafter.draft("followup", caseId, facts) }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("rq_followup")) }
        Button(onClick = { draft = com.cybervictim.assistant.requests.RequestDrafter.draft("docs", caseId, facts) }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("rq_docs")) }
        draft?.let { d ->
            AlertDialog(onDismissRequest = { draft = null }, confirmButton = {
                Button(onClick = {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                        .setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, d)
                    ctx.startActivity(android.content.Intent.createChooser(send, com.cybervictim.assistant.ui.L10n.t("rq_share")))
                    draft = null
                }) { Text(com.cybervictim.assistant.ui.L10n.t("rq_send")) }
            }, title = { Text(com.cybervictim.assistant.ui.L10n.t("rq_ready")) }, text = { Text(d) })
        }
    }
}


@Composable
fun PhoneTraceScreen(nav: androidx.navigation.NavHostController) {
    com.cybervictim.assistant.voice.TrackScreenDescriptor(
        com.cybervictim.assistant.voice.ScreenDescriptor(
            screenId = "phone_trace",
            title = "PhoneTrace Device Security",
            buttons = listOf("Scan Installed Apps", "Usage Access", "Save Baseline", "Compare Baseline"),
            fields = emptyList(),
            owner = com.cybervictim.assistant.portal.StepOwner.AI_AUTO,
            hintKey = "voice_hint_phonetrace"
        )
    )
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var resultsStateLocal by remember { mutableStateOf<List<com.cybervictim.assistant.trc.AppRisk>>(emptyList()) }
    var baselineLocal by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
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
                    text = com.cybervictim.assistant.ui.L10n.t("pt_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Entry point & suspicious app audit",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE2EFEA),
            border = BorderStroke(1.dp, Color(0xFF134E42).copy(alpha = 0.25f))
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = "Forensics Grounding",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF134E42)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = com.cybervictim.assistant.ui.L10n.t("pt_sub"),
                    fontSize = 12.sp,
                    color = Color(0xFF191C1B)
                )
            }
        }

        Text(
            text = "Select Incident Timeframe:",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )

        var pivotIdx by remember { mutableStateOf(2) }
        val pivots = listOf(
            "pt_p1" to (1L * 86400_000L),
            "pt_p2" to (2L * 86400_000L),
            "pt_p3" to (5L * 86400_000L),
            "pt_p4" to (10L * 86400_000L),
            "pt_p5" to (14L * 86400_000L),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            pivots.forEachIndexed { i, (key, _) ->
                FilterChip(
                    selected = pivotIdx == i,
                    onClick = { pivotIdx = i },
                    label = { Text(com.cybervictim.assistant.ui.L10n.t(key), fontSize = 11.sp) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val pivot = System.currentTimeMillis() - pivots[pivotIdx].second
                    val risks = com.cybervictim.assistant.trc.PhoneTrace.scan(ctx, pivot)
                    resultsStateLocal = risks
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF134E42))
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(com.cybervictim.assistant.ui.L10n.t("pt_scan"))
            }

            OutlinedButton(
                onClick = {
                    ctx.startActivity(com.cybervictim.assistant.trc.PhoneTrace.usageAccessIntent(ctx))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (com.cybervictim.assistant.trc.PhoneTrace.usageAccessGranted(ctx))
                        com.cybervictim.assistant.ui.L10n.t("pt_granted")
                    else
                        com.cybervictim.assistant.ui.L10n.t("pt_grant"),
                    fontSize = 11.sp
                )
            }
        }

        val risks = resultsStateLocal
        if (risks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF9FAFB),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Text(
                    text = com.cybervictim.assistant.ui.L10n.t("pt_none"),
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        } else {
            Text(
                text = "Ranked Suspicious Apps (${risks.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            risks.take(6).forEachIndexed { index, r ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(
                        1.dp,
                        if (index == 0 && r.score >= 50) Color(0xFFDC2626) else Color(0xFFE5E7EB)
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("pt_susp"), r.label, r.score),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (index == 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = "Top Suspect",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Risky Access: " + r.riskyAccess.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4B5563)
                        )
                        Text(
                            text = "Package: ${r.pkg}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Usage timeline around fraud time
        val pivot = System.currentTimeMillis() - pivots[pivotIdx].second
        val usage = remember(pivotIdx) { com.cybervictim.assistant.trc.PhoneTrace.usageOpenedAround(ctx, pivot) }
        if (usage.isNotEmpty()) {
            Text(
                text = com.cybervictim.assistant.ui.L10n.t("pt_opened"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    usage.entries.sortedByDescending { it.value }.take(6).forEach { (pkg, n) ->
                        Text(
                            text = java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("pt_opened_n"), pkg, n),
                            fontSize = 12.sp,
                            color = Color(0xFF1F2937)
                        )
                    }
                }
            }
        }

        // Weekly baseline
        Text(
            text = "Baseline Snapshot Comparison:",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val all = ctx.packageManager.getInstalledPackages(0)
                    com.cybervictim.assistant.trc.PhoneTrace.saveBaseline(ctx, risks, all)
                    baselineLocal = com.cybervictim.assistant.trc.PhoneTrace.compareBaseline(ctx)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF134E42))
            ) {
                Text(com.cybervictim.assistant.ui.L10n.t("pt_baseline"), fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { baselineLocal = com.cybervictim.assistant.trc.PhoneTrace.compareBaseline(ctx) },
                modifier = Modifier.weight(1f)
            ) {
                Text(com.cybervictim.assistant.ui.L10n.t("pt_compare"), fontSize = 12.sp)
            }
        }

        baselineLocal?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
            ) {
                Text(it, Modifier.padding(10.dp), fontSize = 12.sp, color = Color(0xFF1F2937))
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = com.cybervictim.assistant.ui.L10n.t("pt_keep"),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFB45309),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ExposureScreen(nav: androidx.navigation.NavHostController) {
    com.cybervictim.assistant.voice.TrackScreenDescriptor(
        com.cybervictim.assistant.voice.ScreenDescriptor(
            screenId = "exposure",
            title = "Exposure & Breach Scanner",
            buttons = listOf("Send Code", "Verify", "Scan Breaches", "Check Password"),
            fields = listOf("Email", "Verification Code", "Password"),
            owner = com.cybervictim.assistant.portal.StepOwner.AI_AUTO,
            hintKey = "voice_hint_exposure"
        )
    )
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = com.cybervictim.assistant.ui.L10n.t("exp_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verify email & check breach exposure",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        Text("First verify it is your email — a code is sent so nobody can scan someone else's email.")

        if (!com.cybervictim.assistant.exposure.ExposureApi.isLinked()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(com.cybervictim.assistant.ui.L10n.t("exp_pending"), style = MaterialTheme.typography.labelLarge)
                    Text(com.cybervictim.assistant.ui.L10n.t("exp_pending_sub"), style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            var email by remember { mutableStateOf("") }
            var code by remember { mutableStateOf("") }
            var verified by remember { mutableStateOf(false) }
            var status by remember { mutableStateOf<String?>(null) }
            var breaches by remember { mutableStateOf<List<org.json.JSONObject>>(emptyList()) }
            val api = com.cybervictim.assistant.exposure.ExposureApi

            TextField(value = email, onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(), label = { Text(com.cybervictim.assistant.ui.L10n.t("exp_email")) })
            Button(onClick = {
                status = com.cybervictim.assistant.ui.L10n.t("exp_sending")
                api.startVerify(email) { r -> status = r }
            }, enabled = email.contains("@")) { Text(com.cybervictim.assistant.ui.L10n.t("exp_send")) }
            TextField(value = code, onValueChange = { code = it },
                modifier = Modifier.fillMaxWidth(), label = { Text(com.cybervictim.assistant.ui.L10n.t("exp_code")) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    api.checkCode(email, code) { r ->
                        if (r.contains("verified", ignoreCase = true) || r == com.cybervictim.assistant.ui.L10n.t("exp_verified")) {
                            status = com.cybervictim.assistant.ui.L10n.t("exp_verified")
                            verified = true
                        } else {
                            status = r
                        }
                    }
                }, enabled = code.isNotBlank()) { Text(com.cybervictim.assistant.ui.L10n.t("exp_verify")) }
                Button(onClick = {
                    status = com.cybervictim.assistant.ui.L10n.t("exp_scanning")
                    api.scanBreaches(email) { r ->
                        status = if (r.ok) null else r.message
                        breaches = r.breaches
                        if (r.ok) {
                            // Phase 6-B: 10-day recurring re-check reminder (local alarm loop)
                            runCatching { com.cybervictim.assistant.remind.ReminderScheduler.scheduleExposureRecheck(ctx) }
                        }
                        if (r.ok && r.message == "not-found") status = com.cybervictim.assistant.ui.L10n.t("exp_notfound")
                    }
                }, enabled = verified) { Text(com.cybervictim.assistant.ui.L10n.t("exp_scan")) }
            }
            status?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            if (breaches.isNotEmpty()) {
                Text(com.cybervictim.assistant.ui.L10n.t("exp_found"), style = MaterialTheme.typography.labelLarge)
                breaches.forEach { b ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp)) {
                            Text(b.optString("name") + " (" + b.optString("date") + ")", style = MaterialTheme.typography.labelLarge)
                            Text(b.optString("what"), style = MaterialTheme.typography.labelSmall, maxLines = 3)
                        }
                    }
                }
                Text(com.cybervictim.assistant.ui.L10n.t("exp_fix"), style = MaterialTheme.typography.labelSmall)
            }
        }

        Text(com.cybervictim.assistant.ui.L10n.t("exp_pw_title"), style = MaterialTheme.typography.labelLarge)
        var pw by remember { mutableStateOf("") }
        var result by remember { mutableStateOf<String?>(null) }
        TextField(value = pw, onValueChange = { pw = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text(com.cybervictim.assistant.ui.L10n.t("exp_pw_label")) })
        Button(onClick = {
            com.cybervictim.assistant.exposure.ExposureChecker.passwordExposed(
                pw,
                { exposed, n ->
                    result = if (exposed)
                        java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("exp_hit"), n)
                    else
                        com.cybervictim.assistant.ui.L10n.t("exp_safe")
                    pw = ""
                },
                { fail -> result = java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("exp_fail"), fail) },
            )
        }, enabled = pw.length >= 4) { Text(com.cybervictim.assistant.ui.L10n.t("exp_pw_check")) }
        result?.let { r -> Card { Text(r, Modifier.padding(10.dp)) } }
    }
}

@Composable
fun SettingsScreen(nav: androidx.navigation.NavHostController) {
    com.cybervictim.assistant.voice.TrackScreenDescriptor(
        com.cybervictim.assistant.voice.ScreenDescriptor(
            screenId = "settings",
            title = "Settings & Privacy",
            buttons = listOf("Change Language", "Status Tracker", "Phone Trace", "Exposure Warning"),
            fields = emptyList(),
            owner = com.cybervictim.assistant.portal.StepOwner.AI_AUTO,
            hintKey = "voice_hint_settings"
        )
    )
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = com.cybervictim.assistant.ui.L10n.t("set_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Preferences & options",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        Text(com.cybervictim.assistant.ui.L10n.t("set_privacy"))
        Text(com.cybervictim.assistant.ui.L10n.t("set_language"), style = MaterialTheme.typography.titleMedium)
        Text(com.cybervictim.assistant.ui.L10n.t("set_language_hint"), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = { nav.navigate(com.cybervictim.assistant.ui.Routes.LANGUAGE_SELECT) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(com.cybervictim.assistant.ui.L10n.t("set_language") + ": " + com.cybervictim.assistant.ui.L10n.lang.label)
        }
        Button(onClick = { nav.navigate(com.cybervictim.assistant.ui.Routes.TRACKER) }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("more_tracker")) }
        Button(onClick = { nav.navigate(com.cybervictim.assistant.ui.Routes.PHONE_TRACE) }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("more_phonetrace")) }
        Button(onClick = { nav.navigate(com.cybervictim.assistant.ui.Routes.EXPOSURE) }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("more_exposure")) }
    }
}
