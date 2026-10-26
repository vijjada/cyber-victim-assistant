package com.cybervictim.assistant.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cybervictim.assistant.BuildConfig
import com.cybervictim.assistant.SuspiciousEmailBridge
import com.cybervictim.assistant.ai.GeminiClient
import com.cybervictim.assistant.data.CaseSession
import com.cybervictim.assistant.email.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// Calm Teal theme palette
private val BgColor = Color(0xFFF7F8F6)
private val PrimaryTeal = Color(0xFF2F6F6A)
private val PrimaryTint = Color(0xFFE3EFEC)
private val LightUrgent = Color(0xFFF5C2B0)
private val DarkText = Color(0xFF263238)
private val SubtitleText = Color(0xFF5A6966)
private val BorderColor = Color(0xFFDCE3E1)
private val CardWhite = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuspiciousEmailScreen(nav: androidx.navigation.NavHostController) {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val caseId = remember { CaseSession.ensure() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = No-inbox-access (default), 1 = Gmail window
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Analysis results list
    val evidenceCards = remember { mutableStateListOf<EmailEvidenceCard>() }

    // Path A state
    var pastedHeadersAndText by remember { mutableStateOf("") }
    var sharedEmailDetected by remember { mutableStateOf(SuspiciousEmailBridge.sharedText != null || SuspiciousEmailBridge.sharedUri != null) }

    // Path B state (Gmail Window)
    var isGmailConnected by remember { mutableStateOf(GmailApiClient.isConnected()) }
    val timeZone = remember { TimeZone.getTimeZone("Asia/Kolkata") }
    val tzFormat = remember {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).apply {
            this.timeZone = timeZone
        }
    }

    // Default incident window: current time +/- 24 hours
    var startWindowMillis by remember { mutableLongStateOf(System.currentTimeMillis() - (24 * 3600 * 1000L)) }
    var endWindowMillis by remember { mutableLongStateOf(System.currentTimeMillis() + (24 * 3600 * 1000L)) }
    var windowError by remember { mutableStateOf<String?>(null) }

    val candidateEmails = remember { mutableStateListOf<CandidateEmail>() }
    var isQueryingCandidates by remember { mutableStateOf(false) }

    // Dialog state for freeze request & gemini explain
    var showFreezeDialog by remember { mutableStateOf(false) }
    var freezeDraftText by remember { mutableStateOf("") }
    var activeAiExplanation by remember { mutableStateOf<Pair<String, String>?>(null) } // Pair(emailId, explanation)
    var explainConsentEmail by remember { mutableStateOf<EmailContent?>(null) }

    // Register OAuth redirect callback
    LaunchedEffect(Unit) {
        SuspiciousEmailBridge.authCodeReceived = { code, state ->
            coroutineScope.launch {
                val clientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID
                val verifier = GmailApiClient.pendingCodeVerifier
                if (clientId.isNotBlank() && verifier != null) {
                    isProcessing = true
                    statusMessage = "Exchanging OAuth authorization code..."
                    val res = withContext(Dispatchers.IO) {
                        GmailOAuthHelper.exchangeCodeForToken(
                            clientId = clientId,
                            redirectUri = GmailOAuthHelper.DEFAULT_REDIRECT_URI,
                            code = code,
                            codeVerifier = verifier
                        )
                    }
                    if (res.isSuccess) {
                        GmailApiClient.setToken(res.getOrNull()!!)
                        isGmailConnected = true
                        statusMessage = "Gmail connected in testing mode (read-only)."
                    } else {
                        statusMessage = "OAuth failed: ${res.exceptionOrNull()?.message}"
                    }
                    isProcessing = false
                }
            }
        }
    }

    // EML file picker for Path A
    val emlPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                statusMessage = "Reading .eml file..."
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    }
                    if (bytes.isNotEmpty()) {
                        val parsed = EmlParser.parse(bytes)
                        val analysis = PhishingAnalyzer.analyze(parsed)
                        val card = EmailStorage.saveAndRecord(ctx, caseId, parsed, analysis)
                        evidenceCards.add(0, card)
                        statusMessage = "EML imported and verified (SHA-256 registered)."
                    } else {
                        statusMessage = "Selected file was empty."
                    }
                } catch (e: Exception) {
                    statusMessage = "Failed to parse EML: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Suspicious Email Check", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        Text("Private on-device phishing analysis", fontSize = 11.sp, color = SubtitleText)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        },
        containerColor = BgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Tabs: Path A (No inbox access) vs Path B (Gmail window)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryTint,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryTeal,
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "No-Inbox-Access (Any Mail)",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) PrimaryTeal else SubtitleText
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Gmail Window (OAuth)",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) PrimaryTeal else SubtitleText
                            )
                        }
                    )
                }
            }

            if (statusMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryTint,
                    border = BorderStroke(1.dp, PrimaryTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage!!,
                        fontSize = 12.sp,
                        color = DarkText,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // =========================================================================
            // PATH A: NO-INBOX-ACCESS (Default, Always Available)
            // =========================================================================
            if (selectedTab == 0) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Import & Analyze Without Mail Access", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        Text(
                            "Your mailbox is never accessed. Share an email from your mail app, pick a saved .eml file, or paste raw headers & text below.",
                            fontSize = 12.sp,
                            color = SubtitleText
                        )

                        // Option 1: Shared intent detection
                        if (sharedEmailDetected) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PrimaryTint,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Email Shared From Mail App", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryTeal)
                                        Text("Ready to analyze on-device", fontSize = 11.sp, color = DarkText)
                                    }
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                isProcessing = true
                                                val text = SuspiciousEmailBridge.sharedText
                                                val uri = SuspiciousEmailBridge.sharedUri
                                                try {
                                                    val parsed = if (uri != null) {
                                                        val bytes = withContext(Dispatchers.IO) {
                                                            ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                                                        }
                                                        EmlParser.parse(bytes)
                                                    } else if (!text.isNullOrBlank()) {
                                                        EmlParser.parse(text)
                                                    } else null

                                                    if (parsed != null) {
                                                        val analysis = PhishingAnalyzer.analyze(parsed)
                                                        val card = EmailStorage.saveAndRecord(ctx, caseId, parsed, analysis)
                                                        evidenceCards.add(0, card)
                                                        sharedEmailDetected = false
                                                        SuspiciousEmailBridge.sharedText = null
                                                        SuspiciousEmailBridge.sharedUri = null
                                                        statusMessage = "Shared email successfully analyzed and saved to case timeline."
                                                    }
                                                } catch (e: Exception) {
                                                    statusMessage = "Analysis failed: ${e.message}"
                                                } finally {
                                                    isProcessing = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                                    ) {
                                        Text("Analyze Now", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Option 2: Pick EML file
                        OutlinedButton(
                            onClick = { emlPickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, PrimaryTeal),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pick .eml File From Storage", fontSize = 12.sp)
                        }

                        // Option 3: Paste headers and text
                        OutlinedTextField(
                            value = pastedHeadersAndText,
                            onValueChange = { pastedHeadersAndText = it },
                            label = { Text("Or paste full email text / raw headers", fontSize = 11.sp) },
                            placeholder = { Text("From: ...\nSubject: ...\n\nEmail body...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (pastedHeadersAndText.isNotBlank()) {
                                        coroutineScope.launch {
                                            isProcessing = true
                                            try {
                                                val parsed = EmlParser.parse(pastedHeadersAndText)
                                                val analysis = PhishingAnalyzer.analyze(parsed)
                                                val card = EmailStorage.saveAndRecord(ctx, caseId, parsed, analysis)
                                                evidenceCards.add(0, card)
                                                pastedHeadersAndText = ""
                                                statusMessage = "Pasted email analyzed and saved as evidence."
                                            } catch (e: Exception) {
                                                statusMessage = "Parse error: ${e.message}"
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    }
                                },
                                enabled = pastedHeadersAndText.isNotBlank() && !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Analyze Pasted Text", fontSize = 12.sp)
                            }

                            // Quick sample test vectors
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val sample = """
                                            From: "SBI Card Support" <security@onlinesbi-kyc-alert.top>
                                            Reply-To: refund-desk@gmail.com
                                            Return-Path: bounce@disposable-mailer.xyz
                                            To: victim@example.com
                                            Subject: URGENT: Your Account Has Been Blocked - Complete KYC
                                            Date: ${SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())}
                                            Authentication-Results: mx.google.com; spf=fail; dmarc=fail
                                            
                                            Dear Customer,
                                            Your netbanking access is suspended immediately due to missing PAN/KYC.
                                            Click here to verify: http://192.168.1.50/sbi-login.php
                                            Failure to update within 2 hours will lead to account termination.
                                        """.trimIndent()
                                        val parsed = EmlParser.parse(sample)
                                        val analysis = PhishingAnalyzer.analyze(parsed)
                                        val card = EmailStorage.saveAndRecord(ctx, caseId, parsed, analysis)
                                        evidenceCards.add(0, card)
                                        statusMessage = "Loaded synthetic test phishing email."
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Load Test Vector", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // PATH B: GMAIL WINDOW (OAuth PKCE, Metadata-Only First)
            // =========================================================================
            if (selectedTab == 1) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gmail Incident Window Search", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
                            if (isGmailConnected) {
                                TextButton(onClick = {
                                    GmailApiClient.disconnect()
                                    isGmailConnected = false
                                    candidateEmails.clear()
                                    statusMessage = "Gmail disconnected and access token revoked."
                                }) {
                                    Text("Disconnect Gmail", color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Restricted scope testing notice
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryTint,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Restricted Scope Notice: In hackathon testing mode, Google OAuth requires test user pre-approval. Only metadata in your chosen window is fetched; full inbox is never read or stored.",
                                fontSize = 11.sp,
                                color = DarkText,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        if (!isGmailConnected) {
                            val clientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID
                            if (clientId.isBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = LightUrgent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(
                                            "OAuth Client ID not set in gradle.properties (GOOGLE_OAUTH_CLIENT_ID).",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkText
                                        )
                                        Text(
                                            "You can still simulate window searching using mock test candidates below.",
                                            fontSize = 11.sp,
                                            color = DarkText
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (clientId.isNotBlank()) {
                                        val verifier = GmailOAuthHelper.generateCodeVerifier()
                                        val challenge = GmailOAuthHelper.generateCodeChallenge(verifier)
                                        val state = GmailOAuthHelper.generateState()
                                        GmailApiClient.pendingCodeVerifier = verifier
                                        GmailApiClient.pendingState = state

                                        val authUrl = GmailOAuthHelper.buildAuthUrl(
                                            clientId = clientId,
                                            codeChallenge = challenge,
                                            state = state
                                        )
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                                        ctx.startActivity(browserIntent)
                                    } else {
                                        // Simulate in-memory session for demonstration/testing
                                        GmailApiClient.setToken("simulated_test_token")
                                        isGmailConnected = true
                                        statusMessage = "Simulated Gmail connected session active."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (clientId.isNotBlank()) "Connect Gmail (Read-Only Scope)" else "Start Demo Gmail Session", fontSize = 12.sp)
                            }
                        } else {
                            // Date-time window selection
                            Text("Select Incident Time Window (${timeZone.id} - IST)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("Window Start (IST)", fontSize = 10.sp, color = SubtitleText)
                                    Text(
                                        tzFormat.format(Date(startWindowMillis)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkText
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Window End (IST)", fontSize = 10.sp, color = SubtitleText)
                                    Text(
                                        tzFormat.format(Date(endWindowMillis)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkText
                                    )
                                }
                            }

                            // Preset buttons
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val now = System.currentTimeMillis()
                                        startWindowMillis = now - (24 * 3600 * 1000L)
                                        endWindowMillis = now + (24 * 3600 * 1000L)
                                        windowError = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text("±24 Hours", fontSize = 10.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val now = System.currentTimeMillis()
                                        startWindowMillis = now - (48 * 3600 * 1000L)
                                        endWindowMillis = now
                                        windowError = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text("Past 2 Days", fontSize = 10.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val now = System.currentTimeMillis()
                                        startWindowMillis = now - (7 * 24 * 3600 * 1000L)
                                        endWindowMillis = now
                                        windowError = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text("Past 7 Days (Max)", fontSize = 10.sp)
                                }
                            }

                            if (windowError != null) {
                                Text(windowError!!, color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val startSec = startWindowMillis / 1000L
                                    val endSec = endWindowMillis / 1000L
                                    val valRes = GmailApiClient.validateWindow(startSec, endSec)
                                    if (valRes.isFailure) {
                                        windowError = valRes.exceptionOrNull()?.message
                                        return@Button
                                    }
                                    windowError = null
                                    coroutineScope.launch {
                                        isQueryingCandidates = true
                                        statusMessage = "Fetching metadata for emails in window (including Spam/Trash)..."
                                        try {
                                            if (GmailApiClient.accessToken == "simulated_test_token") {
                                                // Simulated candidate list for demo/testing without API keys
                                                candidateEmails.clear()
                                                val meta1 = EmailMetadata(
                                                    id = "mock_1",
                                                    from = "SBI Alerts <alert@onlinesbi-kyc.top>",
                                                    replyTo = "scam@disposable.xyz",
                                                    subject = "URGENT: Your Account Has Been Blocked",
                                                    dateStr = tzFormat.format(Date()),
                                                    authResults = "spf=fail; dmarc=fail",
                                                    quickScore = 80,
                                                    quickSuspicionReason = "SPF Failed; Lookalike domain (mimics sbi); Urgent blockage threat"
                                                )
                                                val meta2 = EmailMetadata(
                                                    id = "mock_2",
                                                    from = "Company HR <jobs@career-tech-hiring.live>",
                                                    subject = "Selected for Work From Home - Daily Income ₹5000",
                                                    dateStr = tzFormat.format(Date(System.currentTimeMillis() - 3600000L)),
                                                    authResults = "spf=pass",
                                                    quickScore = 40,
                                                    quickSuspicionReason = "Suspicious .live domain; Task scam lure in subject"
                                                )
                                                candidateEmails.add(CandidateEmail("mock_1", meta1, isSelected = true))
                                                candidateEmails.add(CandidateEmail("mock_2", meta2, isSelected = false))
                                                statusMessage = "Found 2 candidate emails in window. Tick ones to import."
                                            } else {
                                                val res = GmailApiClient.listMetadataOnly(startSec, endSec)
                                                if (res.isSuccess) {
                                                    candidateEmails.clear()
                                                    candidateEmails.addAll(res.getOrNull() ?: emptyList())
                                                    statusMessage = "Found ${candidateEmails.size} candidate emails in window."
                                                } else {
                                                    statusMessage = "Query failed: ${res.exceptionOrNull()?.message}"
                                                }
                                            }
                                        } finally {
                                            isQueryingCandidates = false
                                        }
                                    }
                                },
                                enabled = !isQueryingCandidates,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isQueryingCandidates) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Step 1: Find Emails in Window (Metadata Only)", fontSize = 12.sp)
                            }

                            // Candidate list (Step 1 -> Step 2)
                            if (candidateEmails.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Candidate Emails (${candidateEmails.size} found) - Select to Import:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )

                                candidateEmails.forEach { candidate ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (candidate.isSelected) PrimaryTint else Color(0xFFF9FAFB),
                                        border = BorderStroke(1.dp, if (candidate.isSelected) PrimaryTeal else BorderColor),
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            candidate.isSelected = !candidate.isSelected
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = candidate.isSelected,
                                                onCheckedChange = { candidate.isSelected = it },
                                                colors = CheckboxDefaults.colors(checkedColor = PrimaryTeal)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(candidate.metadata.subject, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                                                Text("From: ${candidate.metadata.from}", fontSize = 11.sp, color = SubtitleText)
                                                Text("Date: ${candidate.metadata.dateStr}", fontSize = 10.sp, color = SubtitleText)
                                                if (!candidate.metadata.quickSuspicionReason.isNullOrBlank()) {
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        candidate.metadata.quickSuspicionReason!!,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (candidate.metadata.quickScore >= 50) Color(0xFFB91C1C) else PrimaryTeal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                val selectedCount = candidateEmails.count { it.isSelected }
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isProcessing = true
                                            statusMessage = "Fetching raw message for selected emails & purging unticked metadata..."
                                            try {
                                                val tickedIds = candidateEmails.filter { it.isSelected }.map { it.id }.toSet()
                                                // Step 2 invariant: discard all unticked candidate metadata from memory immediately
                                                GmailApiClient.discardUnticked(candidateEmails, tickedIds)

                                                for (candidate in candidateEmails.filter { it.isSelected }) {
                                                    val rawText = if (GmailApiClient.accessToken == "simulated_test_token") {
                                                        """
                                                            From: ${candidate.metadata.from}
                                                            Reply-To: ${candidate.metadata.replyTo ?: ""}
                                                            Subject: ${candidate.metadata.subject}
                                                            Date: ${candidate.metadata.dateStr}
                                                            Authentication-Results: ${candidate.metadata.authResults ?: ""}
                                                            
                                                            Simulated raw message content for analysis.
                                                            Visit: http://192.168.1.50/login
                                                        """.trimIndent()
                                                    } else {
                                                        GmailApiClient.fetchRawMessage(candidate.id).getOrThrow()
                                                    }

                                                    val parsed = EmlParser.parse(rawText)
                                                    val analysis = PhishingAnalyzer.analyze(parsed)
                                                    val card = EmailStorage.saveAndRecord(ctx, caseId, parsed, analysis)
                                                    evidenceCards.add(0, card)
                                                }
                                                candidateEmails.clear()
                                                statusMessage = "Imported $selectedCount email(s) into case timeline. Unticked emails discarded."
                                            } catch (e: Exception) {
                                                statusMessage = "Import failed: ${e.message}"
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    enabled = selectedCount > 0 && !isProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Step 2: Import Selected ($selectedCount) & Purge Rest", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // EVIDENCE CARDS: PHISHING ANALYSIS & VICTIM NEXT STEPS
            // =========================================================================
            if (evidenceCards.isNotEmpty()) {
                Text("Analyzed Email Evidence (${evidenceCards.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkText)

                evidenceCards.forEach { card ->
                    EmailEvidenceCardView(
                        card = card,
                        onExplainRequested = { explainConsentEmail = card.emailContent },
                        onFreezeRequested = {
                            freezeDraftText = buildFundFreezeDraft(card)
                            showFreezeDialog = true
                        },
                        activeAiExplanation = activeAiExplanation?.takeIf { it.first == card.emailContent.metadata.id }?.second
                    )
                }
            }
        }
    }

    // Consent dialog for Gemini "Explain simply"
    if (explainConsentEmail != null) {
        val email = explainConsentEmail!!
        AlertDialog(
            onDismissRequest = { explainConsentEmail = null },
            title = { Text("AI Plain-English Explanation", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Send a strictly REDACTED, text-only excerpt of this email to Gemini to explain what it is asking and why it is suspicious?\n\n" +
                    "Privacy guarantee: Personal names, full email addresses, attachments, and headers are NEVER sent.",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toExplain = explainConsentEmail!!
                        explainConsentEmail = null
                        coroutineScope.launch {
                            isProcessing = true
                            try {
                                val redactedText = "Subject: ${toExplain.metadata.subject}\n\n${toExplain.plainTextBody.take(800)}"
                                val prompt = "Explain this suspicious email to a non-technical cybercrime victim in simple, calm bullet points (maximum 3 points). Point out any deception or danger. Do NOT recommend clicking any link:\n\n$redactedText"
                                val explanation = GeminiClient.generateWithFallback(prompt, maxTokens = 350)
                                if (!explanation.isNullOrBlank()) {
                                    activeAiExplanation = Pair(toExplain.metadata.id, explanation)
                                } else {
                                    Toast.makeText(ctx, "AI explanation could not be generated.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "AI explanation unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("I Consent (Explain Simply)")
                }
            },
            dismissButton = {
                TextButton(onClick = { explainConsentEmail = null }) {
                    Text("Cancel", color = SubtitleText)
                }
            }
        )
    }

    // Fund freeze request draft dialog
    if (showFreezeDialog) {
        AlertDialog(
            onDismissRequest = { showFreezeDialog = false },
            title = { Text("Draft Fund Freeze Request", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Copy this formal request and email/deliver it immediately to your bank's nodal officer and cyber cell:",
                        fontSize = 11.sp,
                        color = SubtitleText
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            freezeDraftText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(freezeDraftText))
                        Toast.makeText(ctx, "Freeze request copied to clipboard", Toast.LENGTH_SHORT).show()
                        showFreezeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Copy Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFreezeDialog = false }) {
                    Text("Close", color = SubtitleText)
                }
            }
        )
    }
}

@Composable
private fun EmailEvidenceCardView(
    card: EmailEvidenceCard,
    onExplainRequested: () -> Unit,
    onFreezeRequested: () -> Unit,
    activeAiExplanation: String?
) {
    var expandedTechDetails by remember { mutableStateOf(false) }
    var clickedLinkChecked by remember { mutableStateOf(false) }
    var openedAttachmentChecked by remember { mutableStateOf(false) }
    var moneyInvolvedChecked by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Header: Word-based suspicion rating (NOT traffic lights)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (card.analysis.suspicionLevel) {
                        SuspicionLevel.HIGH_SUSPICION -> LightUrgent
                        SuspicionLevel.SUSPICIOUS -> PrimaryTint
                        SuspicionLevel.LOW_SUSPICION -> Color(0xFFF1F5F9)
                        SuspicionLevel.LEGITIMATE -> Color(0xFFE2EFEA)
                    }
                ) {
                    Text(
                        text = "Rating: ${card.analysis.suspicionLevel.displayText}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text("SHA-256: ${card.emailContent.rawEmlSha256.take(8)}...", fontSize = 10.sp, color = SubtitleText)
            }

            Text(card.emailContent.metadata.subject, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
            Text("From: ${card.emailContent.metadata.from}", fontSize = 11.sp, color = SubtitleText)
            Text("Date: ${card.emailContent.metadata.dateStr}", fontSize = 10.sp, color = SubtitleText)

            // Top 3 reasons
            if (card.analysis.topReasons.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text("Top Reasons for Suspicion:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                card.analysis.topReasons.forEach { reason ->
                    Row(Modifier.padding(vertical = 1.dp)) {
                        Text("• ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                        Text(reason, fontSize = 12.sp, color = DarkText)
                    }
                }
            }

            // Victim Next Steps
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PrimaryTint,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("What You Should Do Next:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                    card.analysis.whatToDoNext.forEach { step ->
                        Text(step, fontSize = 11.sp, color = DarkText)
                    }
                }
            }

            // Interaction / Emergency triage checklist
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFBFBFB),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Did You Interact With This Email?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clickedLinkChecked, onCheckedChange = { clickedLinkChecked = it }, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("I clicked a link or entered a password", fontSize = 11.sp, color = DarkText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = openedAttachmentChecked, onCheckedChange = { openedAttachmentChecked = it }, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("I opened or downloaded an attachment", fontSize = 11.sp, color = DarkText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = moneyInvolvedChecked, onCheckedChange = { moneyInvolvedChecked = it }, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Money or bank details were compromised", fontSize = 11.sp, color = DarkText)
                    }

                    if (clickedLinkChecked || openedAttachmentChecked || moneyInvolvedChecked) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LightUrgent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("URGENT ACTIONS REQUIRED:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkText)
                                Text("1. Immediately change passwords for your email and banking accounts from another device.", fontSize = 10.sp, color = DarkText)
                                Text("2. Disconnect device Wi-Fi/data if a suspicious file or app was downloaded.", fontSize = 10.sp, color = DarkText)
                                Text("3. Dial 1930 immediately to report financial cyber fraud.", fontSize = 10.sp, color = DarkText)

                                Button(
                                    onClick = onFreezeRequested,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Text("Draft Bank Fund Freeze Request", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // AI "Explain simply" section
            if (activeAiExplanation != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryTint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text("AI Plain-English Summary:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryTeal)
                        Spacer(Modifier.height(4.dp))
                        Text(activeAiExplanation, fontSize = 11.sp, color = DarkText)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onExplainRequested,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, PrimaryTeal),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal)
                ) {
                    Text("Explain this email simply (AI with consent)", fontSize = 11.sp)
                }
            }

            // Collapsible technical forensics
            TextButton(
                onClick = { expandedTechDetails = !expandedTechDetails },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (expandedTechDetails) "Hide Technical Forensics ▲" else "View Technical Forensics ▼",
                        fontSize = 11.sp,
                        color = PrimaryTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = expandedTechDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("AUTHENTICATION HEADERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SubtitleText)
                    Text("• SPF: ${card.analysis.spfStatus}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("• DKIM: ${card.analysis.dkimStatus}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("• DMARC: ${card.analysis.dmarcStatus}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                    Spacer(Modifier.height(4.dp))
                    Text("EXTRACTED LINKS (Shown as inert plain text only)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SubtitleText)
                    if (card.emailContent.extractedLinks.isEmpty()) {
                        Text("No links detected in message body.", fontSize = 11.sp, color = SubtitleText)
                    } else {
                        card.emailContent.extractedLinks.forEach { link ->
                            Column(Modifier.padding(vertical = 2.dp)) {
                                Text("Display: ${link.displayText}", fontSize = 10.sp, color = SubtitleText)
                                Text("Target: ${link.targetUrl}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = if (link.isMismatch) Color(0xFFDC2626) else DarkText)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("ATTACHMENTS (Metadata only - never executed)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SubtitleText)
                    if (card.emailContent.attachments.isEmpty()) {
                        Text("No attachments found.", fontSize = 11.sp, color = SubtitleText)
                    } else {
                        card.emailContent.attachments.forEach { att ->
                            Text("• ${att.fileName} (${att.mimeType}, ${att.sizeBytes} bytes)${if (att.isRiskyType) " [RISKY]" else ""}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("SAVED EVIDENCE EML FILE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SubtitleText)
                    Text(card.savedFilePath, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DarkText)
                    Text("SHA-256: ${card.emailContent.rawEmlSha256}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DarkText)
                }
            }
        }
    }
}

private fun buildFundFreezeDraft(card: EmailEvidenceCard): String {
    val meta = card.emailContent.metadata
    val sha = card.emailContent.rawEmlSha256
    return """
        To: The Branch Manager / Nodal Cyber Crime Officer
        Bank: [Insert Bank Name]
        
        Subject: URGENT: Request for Immediate Lien / Fund Freeze on Fraudulent Account
        
        Respected Sir/Madam,
        
        I am writing to urgently report a cyber phishing fraud incident. On ${meta.dateStr}, I received a fraudulent email purporting to be from "${meta.from}" with subject "${meta.subject}".
        
        The email contained deceptive phishing lures that resulted in unauthorized access/transaction.
        
        Forensic details of the phishing email:
        - Sender: ${meta.from}
        - Subject: ${meta.subject}
        - Date: ${meta.dateStr}
        - Cryptographic Hash (SHA-256): $sha
        - Preserved Evidence: .eml stored with original server headers
        
        In accordance with RBI cyber fraud guidelines and Section 91 CrPC / BNSS, I request you to immediately mark a lien / freeze the beneficiary accounts linked to this incident to prevent fund dissipation.
        
        Complainant Name: [Your Name]
        Contact Number: [Your Phone]
        Date: ${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).format(Date())}
    """.trimIndent()
}
