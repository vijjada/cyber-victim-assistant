package com.cybervictim.assistant.ui.screens

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.cybervictim.assistant.ai.CaseSessionStore
import com.cybervictim.assistant.ai.ComplaintScriptGenerator
import com.cybervictim.assistant.data.*
import com.cybervictim.assistant.portal.StepOwner
import com.cybervictim.assistant.sms.ExtractedScamData
import com.cybervictim.assistant.sms.SmsScamExtractor
import com.cybervictim.assistant.ui.L10n
import com.cybervictim.assistant.ui.Routes
import com.cybervictim.assistant.voice.ScreenDescriptor
import com.cybervictim.assistant.voice.TrackScreenDescriptor
import com.cybervictim.assistant.voice.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BgColor = Color(0xFFF7F8F4)
private val TealPrimary = Color(0xFF134E42)
private val TealHalo = Color(0xFFE2EFEA)
private val DarkText = Color(0xFF191C1B)
private val SubtitleGray = Color(0xFF6B7280)
private val BorderColor = Color(0xFFE5E7EB)
private val CardBg = Color.White
private val TagBg = Color(0xFFF3F4F6)

data class GuidedStep(
    val question: String,
    val explanation: String,
    val portalField: String,
    val factType: String
)

@Composable
fun GuidedQuestionScreen(nav: NavHostController, category: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    // Permission launcher for READ_SMS
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "SMS permission granted! Scanning inbox for scam data...", Toast.LENGTH_SHORT).show()
        }
    }

    // Step definitions based on chosen category — 8 structured steps
    val steps = remember(category) {
        when (category) {
            "hacked" -> listOf(
                GuidedStep(
                    question = "Which platform or account was hacked?",
                    explanation = "Tell us the service (e.g. Gmail, Instagram, WhatsApp, Facebook, Telegram).",
                    portalField = "Platform / Application",
                    factType = "app"
                ),
                GuidedStep(
                    question = "What is your username or account ID?",
                    explanation = "The handle, registered phone number or email you used to log in.",
                    portalField = "Victim Account Identifier",
                    factType = "account_id"
                ),
                GuidedStep(
                    question = "When did you lose access to the account?",
                    explanation = "Say the date and time as best you remember. An approximate time is fine.",
                    portalField = "Date and time incident occurred",
                    factType = "timestamp"
                ),
                GuidedStep(
                    question = "What phone number or email was linked to the account?",
                    explanation = "The recovery details previously associated with your account.",
                    portalField = "Recovery Phone / Email",
                    factType = "phone"
                ),
                GuidedStep(
                    question = "Did you receive any password reset OTP or verification link?",
                    explanation = "e.g. Received an SMS OTP, or clicked a phishing link sent by someone.",
                    portalField = "Method of Compromise",
                    factType = "other"
                ),
                GuidedStep(
                    question = "How did the fraudster contact or deceive you?",
                    explanation = "Describe the contact method or malicious technique used.",
                    portalField = "Method of Contact",
                    factType = "other"
                ),
                GuidedStep(
                    question = "Did you install any remote-control app or malicious file?",
                    explanation = "Mention any APK, screen sharing app or link clicked.",
                    portalField = "Method of Compromise / App Installed",
                    factType = "other"
                ),
                GuidedStep(
                    question = "Briefly describe what happened in your own words",
                    explanation = "AI will understand the scenario and create the official complaint script.",
                    portalField = "Incident Additional Information",
                    factType = "other"
                )
            )
            "phishing" -> listOf(
                GuidedStep(
                    question = "What was the suspect's phone number, email, or link?",
                    explanation = "The suspect's number, fake website link, or email sender address.",
                    portalField = "Suspect Phone / Website URL",
                    factType = "phone"
                ),
                GuidedStep(
                    question = "Which bank or account was targeted?",
                    explanation = "The bank or wallet details the fraudster attempted to exploit.",
                    portalField = "Bank Name from which payment was made",
                    factType = "other"
                ),
                GuidedStep(
                    question = "When did you receive the call or message?",
                    explanation = "Date and approximate time when the contact occurred.",
                    portalField = "Date and time of incident",
                    factType = "timestamp"
                ),
                GuidedStep(
                    question = "What is the fraudster's UPI ID, phone or beneficiary info?",
                    explanation = "The destination UPI ID, link or number where details were sent.",
                    portalField = "Account No. / Fraudster UPI",
                    factType = "upi_id"
                ),
                GuidedStep(
                    question = "Do you have the Transaction ID or UTR number?",
                    explanation = "Check your SMS or bank statement for a 12-digit UTR reference.",
                    portalField = "Transaction ID / UTR reference",
                    factType = "txn_ref"
                ),
                GuidedStep(
                    question = "How did the fraudster contact you?",
                    explanation = "Select or type the communication channel used.",
                    portalField = "Method of Contact",
                    factType = "other"
                ),
                GuidedStep(
                    question = "Did you click any link or install a remote app?",
                    explanation = "e.g. AnyDesk, TeamViewer, RustDesk, or a fake APK file.",
                    portalField = "Method of Compromise / App Installed",
                    factType = "other"
                ),
                GuidedStep(
                    question = "Briefly describe what happened in your own words",
                    explanation = "AI will understand the scenario and create the official complaint script.",
                    portalField = "Incident Additional Information",
                    factType = "other"
                )
            )
            else -> listOf(
                // Default: Money lost through UPI or bank — 8 Steps matching reference exactly
                GuidedStep(
                    question = "How much money was lost?",
                    explanation = "Enter the total amount in Rupees transferred to the fraudster.",
                    portalField = "Amount Paid",
                    factType = "amount"
                ),
                GuidedStep(
                    question = "Which bank or app did you pay from?",
                    explanation = "e.g. State Bank of India, HDFC, ICICI, Google Pay, PhonePe, Paytm, Bank of Baroda.",
                    portalField = "Bank Name from which payment was made",
                    factType = "other"
                ),
                GuidedStep(
                    // Exactly matches reference screenshot: Step 3 of 8
                    question = "When did the money leave your account?",
                    explanation = "Say the date and time as best you remember. An approximate time is fine.",
                    portalField = "Date and time of incident",
                    factType = "timestamp"
                ),
                GuidedStep(
                    question = "What is the fraudster's UPI ID, phone or account?",
                    explanation = "The recipient UPI handle, bank account number, or phone number where money was sent.",
                    portalField = "Account No. / Fraudster UPI",
                    factType = "upi_id"
                ),
                GuidedStep(
                    question = "Do you have the Transaction ID or UTR number?",
                    explanation = "Check your SMS or bank statement for a 12-digit UTR reference.",
                    portalField = "Transaction ID / UTR reference",
                    factType = "txn_ref"
                ),
                GuidedStep(
                    question = "How did the fraudster contact you?",
                    explanation = "Select from AI suggestions or type your own response.",
                    portalField = "Method of Contact",
                    factType = "other"
                ),
                GuidedStep(
                    question = "Did you click any link or install a remote app?",
                    explanation = "Select from AI suggestions or type your own response.",
                    portalField = "Method of Compromise / App Installed",
                    factType = "other"
                ),
                GuidedStep(
                    question = "Briefly describe how the fraud occurred",
                    explanation = "AI will understand the entire scenario and generate the official police complaint script ready to upload in one click.",
                    portalField = "Incident Additional Information",
                    factType = "other"
                )
            )
        }
    }

    var stepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[stepIndex]

    TrackScreenDescriptor(
        ScreenDescriptor(
            screenId = "guided_question",
            title = "Guided Question",
            stepIndex = stepIndex,
            stepTitle = currentStep.portalField,
            buttons = listOf("Next", "Back", "Listen"),
            fields = listOf(currentStep.portalField),
            owner = StepOwner.AI_AUTO,
            hintKey = "voice_hint_guided_q"
        )
    )

    // Answers dictionary to hold all answers across steps 1-8
    val collectedAnswers = remember { mutableStateMapOf<Int, String>() }
    var answerText by remember(stepIndex) {
        mutableStateOf(collectedAnswers[stepIndex] ?: "")
    }

    // Extracted SMS data state (persisted across all steps)
    var lastExtractedScam by remember { mutableStateOf<ExtractedScamData?>(null) }
    var uploadedEvidenceLabel by remember { mutableStateOf<String?>(null) }

    // AI Generated Script state for Step 8
    var aiGeneratedScript by remember { mutableStateOf("") }
    var isGeneratingScript by remember { mutableStateOf(false) }

    // File/Image upload launcher for Step 6 and Step 7
    val evidencePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val caseId = CaseSession.ensure()
            val db = CaseDb.get(context)
            val stepLabel = if (stepIndex == 5) "Contact/Chat Screenshot" else "Technical/Link Screenshot"
            coroutineScope.launch(Dispatchers.IO) {
                db.dao().addEvidence(
                    EvidenceItem(
                        caseId = caseId,
                        displayName = "$stepLabel (${System.currentTimeMillis().toString(36)})",
                        uri = uri.toString(),
                        kind = "screenshot",
                        extractedFactIds = "[]"
                    )
                )
            }
            uploadedEvidenceLabel = "Evidence uploaded & linked to complaint"
            Toast.makeText(context, "Evidence uploaded! Added to complaint filing.", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-scan SMS when date & time is active
    val isDateTimeStep = currentStep.factType == "timestamp"
    LaunchedEffect(isDateTimeStep, answerText) {
        if (isDateTimeStep) {
            withContext(Dispatchers.IO) {
                val results = SmsScamExtractor.scanInboxForScamData(context, answerText)
                if (results.isNotEmpty()) {
                    lastExtractedScam = results.first()
                }
            }
        }
    }

    // In Step 8, automatically synthesize the official complaint script using all facts
    LaunchedEffect(stepIndex) {
        if (stepIndex == 7 && aiGeneratedScript.isBlank()) {
            val profile = UserProfileStore.get(context)
            val amt = collectedAnswers[0]?.ifBlank { null } ?: lastExtractedScam?.amount ?: ""
            val bnk = collectedAnswers[1]?.ifBlank { null } ?: lastExtractedScam?.bankName ?: ""
            val dt = collectedAnswers[2]?.ifBlank { null } ?: lastExtractedScam?.dateTimeFormatted ?: ""
            val frd = collectedAnswers[3]?.ifBlank { null } ?: lastExtractedScam?.fraudsterId ?: ""
            val utr = collectedAnswers[4]?.ifBlank { null } ?: lastExtractedScam?.utrRef ?: ""
            val contact = collectedAnswers[5]?.ifBlank { null } ?: ""
            val compromise = collectedAnswers[6]?.ifBlank { null } ?: ""

            // Instant on-device legal narrative
            val instantNarrative = ComplaintScriptGenerator.buildLegalNarrative(
                profile = profile,
                amount = amt,
                bank = bnk,
                dateTime = dt,
                fraudster = frd,
                utr = utr,
                contactMethod = contact,
                compromiseMethod = compromise
            )
            aiGeneratedScript = instantNarrative
            answerText = instantNarrative

            // Asynchronously attempt background refinement without blocking
            isGeneratingScript = true
            coroutineScope.launch {
                val script = ComplaintScriptGenerator.generateScript(
                    profile = profile,
                    amount = amt,
                    bank = bnk,
                    dateTime = dt,
                    fraudster = frd,
                    utr = utr,
                    contactMethod = contact,
                    compromiseMethod = compromise
                )
                if (script.isNotBlank() && script != instantNarrative) {
                    aiGeneratedScript = script
                    answerText = script
                }
                isGeneratingScript = false
            }
        }
    }

    fun saveCurrentFactAndAdvance() {
        val caseId = CaseSession.ensure()
        val text = answerText.trim()
        collectedAnswers[stepIndex] = text

        if (text.isNotBlank()) {
            CaseSessionStore.current?.onUserMessage("${currentStep.portalField}: $text")
            val db = CaseDb.get(context)
            coroutineScope.launch {
                db.dao().addFact(
                    CaseFact(
                        caseId = caseId,
                        type = currentStep.factType,
                        valueText = text,
                        sourceEvidenceId = 0L,
                        confidence = 1f
                    )
                )

                if (isDateTimeStep) {
                    lastExtractedScam?.let { SmsScamExtractor.applyToCase(context, it) }
                }
            }
        }

        if (stepIndex < steps.size - 1) {
            stepIndex += 1
            uploadedEvidenceLabel = null
        } else {
            // STEP 8 COMPLETE: 1-Click Auto-Fill Portal Clone!
            Toast.makeText(context, "Complaint script ready! Launching Cyber Crime Portal Clone...", Toast.LENGTH_LONG).show()
            nav.navigate(Routes.PORTAL_CLONE) {
                popUpTo(Routes.LANGUAGE_SELECT) { inclusive = false }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Navigation: "< Back" and "Step X of 8"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (stepIndex > 0) {
                            stepIndex -= 1
                            uploadedEvidenceLabel = null
                        } else {
                            nav.popBackStack()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = DarkText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "Back",
                        fontSize = 15.sp,
                        color = DarkText,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Step ${stepIndex + 1} of ${steps.size}",
                    fontSize = 14.sp,
                    color = SubtitleGray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (stepIndex + 1).toFloat() / steps.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = TealPrimary,
                trackColor = Color(0xFFE5E7EB),
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(24.dp))

            // Card 1: YOUR GUIDE SAYS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = CardBg,
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header row: "YOUR GUIDE SAYS" + "Listen" button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (stepIndex == 7) "AI COMPLAINT SYNTHESIZER" else "YOUR GUIDE SAYS",
                            color = TealPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderColor),
                            color = Color.White,
                            modifier = Modifier.clickable {
                                TtsManager.speak(
                                    text = "${currentStep.question}. ${currentStep.explanation}",
                                    languageTag = L10n.lang.toBcp47()
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpeakerVector(
                                    tint = DarkText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Listen",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkText
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Question title
                    Text(
                        text = currentStep.question,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        lineHeight = 28.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    // Explanation
                    Text(
                        text = currentStep.explanation,
                        fontSize = 14.sp,
                        color = SubtitleGray,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    // Portal field tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TagBg
                    ) {
                        Text(
                            text = "Portal field: ${currentStep.portalField}",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // =========================================================================
            // 1. EXTRACTED SMS ONE-TAP FILL BUTTON (Used on relevant steps like UTR, UPI ID, Bank, Amount)
            // =========================================================================
            val scam = lastExtractedScam
            val smsFillValue: String? = when (stepIndex) {
                0 -> scam?.amount // Amount
                1 -> scam?.bankName // Bank Name
                2 -> scam?.dateTimeFormatted // Date & Time
                3 -> scam?.fraudsterId // Fraudster UPI / Account
                4 -> scam?.utrRef // UTR / Txn Ref
                else -> null
            }

            if (smsFillValue != null && smsFillValue.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            answerText = smsFillValue
                            Toast.makeText(context, "Filled from detected Bank SMS!", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "DETECTED IN BANK SMS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "Fill: $smsFillValue",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF059669)
                        ) {
                            Text(
                                text = "Use",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // =========================================================================
            // 2. STEP 3: SMS INBOX SCAM EXTRACTOR FULL CARD
            // =========================================================================
            if (isDateTimeStep) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFF0F766E)),
                    shadowElevation = 2.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "SMS INBOX SCAM EXTRACTOR",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E),
                                    letterSpacing = 1.sp
                                )
                            }

                            if (!hasSmsPermission) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TealHalo,
                                    modifier = Modifier.clickable {
                                        smsPermissionLauncher.launch(android.Manifest.permission.READ_SMS)
                                    }
                                ) {
                                    Text(
                                        "Grant SMS Permission",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TealPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "When date & time is mentioned, our app scans your inbox for bank/UPI debit messages and extracts the scam transaction details:",
                            fontSize = 12.sp,
                            color = SubtitleGray,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        if (scam != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            scam.bankName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF14532D)
                                        )
                                        Text(
                                            scam.amount,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFFB91C1C)
                                        )
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Text("Date: ${scam.dateTimeFormatted}", fontSize = 12.sp, color = DarkText)
                                    Text("UTR / Txn Ref: ${scam.utrRef}", fontSize = 12.sp, color = DarkText)
                                    Text("Beneficiary: ${scam.fraudsterId}", fontSize = 12.sp, color = DarkText)

                                    Spacer(Modifier.height(6.dp))

                                    Text("SMS: \"${scam.smsBody}\"", fontSize = 11.sp, color = SubtitleGray, lineHeight = 15.sp)
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    answerText = scam.dateTimeFormatted
                                    SmsScamExtractor.applyToCase(context, scam)
                                    Toast.makeText(
                                        context,
                                        "Scam data auto-filled: ${scam.amount} via ${scam.bankName} (UTR: ${scam.utrRef})",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Text("Auto-Fill Extracted Scam Data", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            // =========================================================================
            // 3. STEP 6 & STEP 7: AI SMART OPTION CHIPS & EVIDENCE UPLOAD
            // =========================================================================
            if (stepIndex == 5) {
                // Step 6: Contact Method Options
                Text(
                    text = "SUGGESTED CONTACT METHODS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))

                val contactOptions = listOf(
                    "Telegram work-from-home task group",
                    "WhatsApp job offer message",
                    "Phone call pretending to be bank manager",
                    "SMS with electricity bill disconnection link",
                    "Instagram / Facebook ad investment scheme"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    contactOptions.forEach { opt ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { answerText = opt },
                            shape = RoundedCornerShape(10.dp),
                            color = if (answerText == opt) TealHalo else Color.White,
                            border = BorderStroke(1.dp, if (answerText == opt) TealPrimary else BorderColor)
                        ) {
                            Text(
                                text = opt,
                                fontSize = 13.sp,
                                color = DarkText,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Evidence Upload Card for Step 6
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "RECOMMENDED EVIDENCE UPLOAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Upload screenshot of WhatsApp chat, Telegram group, or call log where the fraudster contacted you.",
                            fontSize = 12.sp,
                            color = SubtitleGray
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { evidencePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Text("Upload Chat / Contact Screenshot")
                        }
                        uploadedEvidenceLabel?.let { label ->
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(label, fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                if (category == "phishing" || category == "hacked" || category == "job") {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFE3EFEC),
                        border = BorderStroke(1.dp, Color(0xFF2F6F6A)),
                        modifier = Modifier.fillMaxWidth().clickable {
                            nav.navigate(Routes.SUSPICIOUS_EMAIL)
                        }
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Suggested step: Check Suspicious Email",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2F6F6A)
                                )
                                Text(
                                    "Analyze emails received around the incident window for phishing links & spoofing indicators.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF191C1B)
                                )
                            }
                            Text("→", fontSize = 18.sp, color = Color(0xFF2F6F6A), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            if (stepIndex == 6) {
                // Step 7: Method of Compromise Options
                Text(
                    text = "SUGGESTED COMPROMISE TECHNIQUES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))

                val compromiseOptions = listOf(
                    "Installed AnyDesk / TeamViewer / RustDesk remote app",
                    "Downloaded fake APK file from link or WhatsApp",
                    "Clicked phishing link and entered UPI PIN",
                    "Shared OTP under urgency or fear",
                    "Direct UPI transfer without downloading apps"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    compromiseOptions.forEach { opt ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { answerText = opt },
                            shape = RoundedCornerShape(10.dp),
                            color = if (answerText == opt) TealHalo else Color.White,
                            border = BorderStroke(1.dp, if (answerText == opt) TealPrimary else BorderColor)
                        ) {
                            Text(
                                text = opt,
                                fontSize = 13.sp,
                                color = DarkText,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Evidence Upload Card for Step 7
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "RECOMMENDED TECHNICAL EVIDENCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Upload screenshot of the phishing link, payment receipt, or downloaded app for cyber forensics.",
                            fontSize = 12.sp,
                            color = SubtitleGray
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { evidencePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Text("Upload Technical Evidence / Receipt")
                        }
                        uploadedEvidenceLabel?.let { label ->
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(label, fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                if (category == "phishing" || category == "hacked" || category == "job") {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFE3EFEC),
                        border = BorderStroke(1.dp, Color(0xFF2F6F6A)),
                        modifier = Modifier.fillMaxWidth().clickable {
                            nav.navigate(Routes.SUSPICIOUS_EMAIL)
                        }
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Suggested step: Check Suspicious Email",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2F6F6A)
                                )
                                Text(
                                    "Analyze emails received around the incident window for phishing links & spoofing indicators.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF191C1B)
                                )
                            }
                            Text("→", fontSize = 18.sp, color = Color(0xFF2F6F6A), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            // =========================================================================
            // 4. STEP 8: SYNTHESIZED OFFICIAL COMPLAINT SCRIPT (1-CLICK UPLOAD READY)
            // =========================================================================
            if (stepIndex == 7) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.5.dp, Color(0xFF059669)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "OFFICIAL COMPLAINT SCRIPT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            isGeneratingScript = true
                                            val profile = UserProfileStore.get(context)
                                            val amt = collectedAnswers[0]?.ifBlank { null } ?: lastExtractedScam?.amount ?: ""
                                            val bnk = collectedAnswers[1]?.ifBlank { null } ?: lastExtractedScam?.bankName ?: ""
                                            val dt = collectedAnswers[2]?.ifBlank { null } ?: lastExtractedScam?.dateTimeFormatted ?: ""
                                            val frd = collectedAnswers[3]?.ifBlank { null } ?: lastExtractedScam?.fraudsterId ?: ""
                                            val utr = collectedAnswers[4]?.ifBlank { null } ?: lastExtractedScam?.utrRef ?: ""
                                            val contact = collectedAnswers[5]?.ifBlank { null } ?: ""
                                            val compromise = collectedAnswers[6]?.ifBlank { null } ?: ""

                                            val script = ComplaintScriptGenerator.generateScript(
                                                profile, amt, bnk, dt, frd, utr, contact, compromise
                                            )
                                            aiGeneratedScript = script
                                            answerText = script
                                            isGeneratingScript = false
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Regenerate", fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(answerText))
                                        Toast.makeText(context, "Script copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Copy", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "AI analyzed your incident facts (amount, bank, date, UTR, suspect UPI, and method) and prepared this complaint script ready to upload to cybercrime.gov.in in one click:",
                            fontSize = 12.sp,
                            color = Color(0xFF047857),
                            lineHeight = 16.sp
                        )

                        if (isGeneratingScript) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF059669))
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            // Card 2: Your answer (Editable box)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { focusRequester.requestFocus() },
                shape = RoundedCornerShape(16.dp),
                color = CardBg,
                border = BorderStroke(1.5.dp, TealPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = if (stepIndex == 7) "Complaint Narrative (Editable)" else "Your answer",
                        fontSize = 13.sp,
                        color = SubtitleGray
                    )

                    Spacer(Modifier.height(6.dp))

                    OutlinedTextField(
                        value = answerText,
                        onValueChange = {
                            answerText = it
                            collectedAnswers[stepIndex] = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(if (stepIndex == 7) "Complaint narrative will appear here..." else "Type your answer here...")
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = DarkText,
                            fontSize = if (stepIndex == 7) 14.sp else 16.sp,
                            fontWeight = if (stepIndex == 7) FontWeight.Normal else FontWeight.Medium,
                            lineHeight = if (stepIndex == 7) 20.sp else 22.sp
                        ),
                        minLines = if (stepIndex == 7) 5 else 1,
                        maxLines = if (stepIndex == 7) 10 else 4
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action row: "Type instead" on left, circular mic button on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        focusRequester.requestFocus()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    modifier = Modifier.height(46.dp)
                ) {
                    Text(
                        text = "Type instead",
                        color = DarkText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(TealHalo)
                        .clickable {
                            Toast.makeText(context, "Voice input coming in future update", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TealPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        MicVector(
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            // Main Action Button: "Looks right, continue" OR "1-Click Auto-Fill Portal" on Step 8
            Button(
                onClick = { saveCurrentFactAndAdvance() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stepIndex == steps.size - 1) Color(0xFF059669) else TealPrimary
                )
            ) {
                Text(
                    text = if (stepIndex == steps.size - 1) "1-Click Auto-Fill Portal" else "Looks right, continue",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Secondary Action Button: "Skip for now"
            OutlinedButton(
                onClick = {
                    if (stepIndex < steps.size - 1) {
                        stepIndex += 1
                        uploadedEvidenceLabel = null
                    } else {
                        nav.navigate(Routes.REGISTER_HUB) {
                            popUpTo(Routes.REGISTER_HUB) { inclusive = false }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderColor),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Text(
                    text = if (stepIndex == steps.size - 1) "Save to Dashboard" else "Skip for now",
                    color = DarkText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Crisp Canvas speaker icon matching the reference screenshot */
@Composable
fun SpeakerVector(modifier: Modifier = Modifier, tint: Color = DarkText) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Speaker horn shape
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.35f)
            lineTo(w * 0.35f, h * 0.35f)
            lineTo(w * 0.62f, h * 0.15f)
            lineTo(w * 0.62f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.65f)
            lineTo(w * 0.12f, h * 0.65f)
            close()
        }
        drawPath(path = path, color = tint)

        // Arc wave 1
        drawArc(
            color = tint,
            startAngle = -40f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(w * 0.50f, h * 0.28f),
            size = Size(w * 0.32f, h * 0.44f),
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
        )
        // Arc wave 2
        drawArc(
            color = tint,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.62f, h * 0.16f),
            size = Size(w * 0.34f, h * 0.68f),
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
        )
    }
}

