package com.cybervictim.assistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RequestsScreen(nav: androidx.navigation.NavHostController) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(com.cybervictim.assistant.ui.L10n.t("rq_title"), style = MaterialTheme.typography.titleLarge)
        Text(com.cybervictim.assistant.ui.L10n.t("rq_sub"))
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var resultsStateLocal by remember { mutableStateOf<List<com.cybervictim.assistant.trc.AppRisk>>(emptyList()) }
    var baselineLocal by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(com.cybervictim.assistant.ui.L10n.t("pt_title"), style = MaterialTheme.typography.titleLarge)
        Text(com.cybervictim.assistant.ui.L10n.t("pt_sub"), style = MaterialTheme.typography.labelMedium)
        var pivotIdx by remember { mutableStateOf(2) }
        val pivots = listOf(
            "pt_p1" to (1L * 86400_000L),
            "pt_p2" to (2L * 86400_000L),
            "pt_p3" to (5L * 86400_000L),
            "pt_p4" to (10L * 86400_000L),
            "pt_p5" to (14L * 86400_000L),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            pivots.forEachIndexed { i, (key, _) ->
                FilterChip(selected = pivotIdx == i, onClick = { pivotIdx = i }, label = { Text(com.cybervictim.assistant.ui.L10n.t(key)) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val pivot = System.currentTimeMillis() - pivots[pivotIdx].second
                val risks = com.cybervictim.assistant.trc.PhoneTrace.scan(ctx, pivot)
                resultsStateLocal = risks
            }) { Text(com.cybervictim.assistant.ui.L10n.t("pt_scan")) }
            TextButton(onClick = {
                ctx.startActivity(com.cybervictim.assistant.trc.PhoneTrace.usageAccessIntent(ctx))
            }) { Text(if (com.cybervictim.assistant.trc.PhoneTrace.usageAccessGranted(ctx)) com.cybervictim.assistant.ui.L10n.t("pt_granted") else com.cybervictim.assistant.ui.L10n.t("pt_grant")) }
        }
        val risks = resultsStateLocal
        if (risks.isEmpty()) Text(com.cybervictim.assistant.ui.L10n.t("pt_none"))
        risks.take(4).forEach { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    Text(java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("pt_susp"), r.label, r.score), style = MaterialTheme.typography.labelLarge)
                    Text(r.riskyAccess.joinToString(", "), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        // usage timeline
        val pivot = System.currentTimeMillis() - pivots[pivotIdx].second
        val usage = remember(pivotIdx) { com.cybervictim.assistant.trc.PhoneTrace.usageOpenedAround(ctx, pivot) }
        if (usage.isNotEmpty()) {
            Text(com.cybervictim.assistant.ui.L10n.t("pt_opened"), style = MaterialTheme.typography.labelLarge)
            usage.entries.sortedByDescending { it.value }.take(5).forEach { (pkg, n) ->
                Text(java.lang.String.format(com.cybervictim.assistant.ui.L10n.t("pt_opened_n"), pkg, n))
            }
        }
        // weekly baseline
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val all = ctx.packageManager.getInstalledPackages(0)
                com.cybervictim.assistant.trc.PhoneTrace.saveBaseline(ctx, risks, all)
                baselineLocal = com.cybervictim.assistant.trc.PhoneTrace.compareBaseline(ctx)
            }) { Text(com.cybervictim.assistant.ui.L10n.t("pt_baseline")) }
            TextButton(onClick = { baselineLocal = com.cybervictim.assistant.trc.PhoneTrace.compareBaseline(ctx) }) { Text(com.cybervictim.assistant.ui.L10n.t("pt_compare")) }
        }
        baselineLocal?.let { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(8.dp)) } }
    }
    Text(com.cybervictim.assistant.ui.L10n.t("pt_keep"), style = MaterialTheme.typography.labelSmall)
}

@Composable
fun ExposureScreen(nav: androidx.navigation.NavHostController) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(com.cybervictim.assistant.ui.L10n.t("exp_title"), style = MaterialTheme.typography.titleLarge)
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
                        status = r
                        if (r == com.cybervictim.assistant.ui.L10n.t("exp_verified")) verified = true
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(com.cybervictim.assistant.ui.L10n.t("set_title"), style = MaterialTheme.typography.titleLarge)
        Text(com.cybervictim.assistant.ui.L10n.t("set_privacy"))
        Text(com.cybervictim.assistant.ui.L10n.t("set_language"), style = MaterialTheme.typography.titleMedium)
        Text(com.cybervictim.assistant.ui.L10n.t("set_language_hint"), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.cybervictim.assistant.ui.L10n.Lang.entries.forEach { lg ->
                FilterChip(
                    selected = com.cybervictim.assistant.ui.L10n.lang == lg,
                    onClick = { com.cybervictim.assistant.ui.L10n.setLang(ctx, lg) },
                    label = { Text(lg.label) },
                )
            }
        }
        Button(onClick = { nav.navigate(com.cybervictim.assistant.ui.Routes.TRACKER) }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("more_tracker")) }
        Button(onClick = { nav.navigate(com.cybervictim.assistant.ui.Routes.PHONE_TRACE) }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("more_phonetrace")) }
        Button(onClick = { nav.navigate(com.cybervictim.assistant.ui.Routes.EXPOSURE) }, Modifier.fillMaxWidth()) { Text(com.cybervictim.assistant.ui.L10n.t("more_exposure")) }
    }
}
