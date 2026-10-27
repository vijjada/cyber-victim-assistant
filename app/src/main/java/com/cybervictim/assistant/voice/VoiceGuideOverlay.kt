package com.cybervictim.assistant.voice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cybervictim.assistant.portal.StepOwner
import com.cybervictim.assistant.ui.L10n

private val DeepNavyBlue = Color(0xFF082B5B)
private val RoyalBlue = Color(0xFF145DA0)
private val SoftMint = Color(0xFFDDF3E8)
private val DarkText = Color(0xFF263238)
private val SubtitleGray = Color(0xFF546E7A)
private val BorderColor = Color(0xFFE5E7EB)
private val WarningAmber = Color(0xFFF59E0B)

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    GUIDING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceGuideOverlay() {
    val context = LocalContext.current
    val descriptor = ScreenDescriptorRegistry.currentDescriptor.value ?: return

    var isSheetOpen by remember { mutableStateOf(false) }
    var voiceState by remember { mutableStateOf(VoiceState.IDLE) }
    var transcript by remember { mutableStateOf("") }
    var currentGuidance by remember { mutableStateOf("") }
    var typedQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var currentRms by remember { mutableFloatStateOf(0f) }
    var isSpeaking by remember { mutableStateOf(false) }
    var speechAvailable by remember { mutableStateOf(true) }
    val history = remember { mutableStateListOf<VoiceGuideMessage>() }

    val sttEngine = remember { AndroidSttEngine(context) }

    // Init TTS
    LaunchedEffect(Unit) {
        TtsManager.init(context)
        speechAvailable = sttEngine.isAvailable()
    }

    // Common transcript submission logic
    fun submitTranscript(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        errorMessage = ""
        transcript = trimmed
        voiceState = VoiceState.THINKING
        history.add(VoiceGuideMessage("user", trimmed))
        VoiceGuideClient.requestGuidance(
            transcript = trimmed,
            languageTag = L10n.lang.toBcp47(),
            descriptor = descriptor,
            history = history,
            onSuccess = { reply ->
                currentGuidance = reply
                voiceState = VoiceState.GUIDING
                history.add(VoiceGuideMessage("assistant", reply))
                isSpeaking = true
                TtsManager.speak(
                    text = reply,
                    languageTag = L10n.lang.toBcp47(),
                    onStart = { isSpeaking = true },
                    // FIX 1: reset to IDLE after TTS so mic is available again
                    onDone = {
                        isSpeaking = false
                        voiceState = VoiceState.IDLE
                    },
                    onError = { errMsg ->
                        isSpeaking = false
                        voiceState = VoiceState.IDLE
                        // FIX 3: silent degrade when TTS lang unavailable — text still shows
                        if (errMsg != "TTS_LANG_UNAVAILABLE") {
                            errorMessage = errMsg
                        }
                    }
                )
            },
            onError = {
                voiceState = VoiceState.IDLE
            }
        )
    }

    // System voice input launcher (100% reliable fallback across all Android OEM ROMs)
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = spokenList?.firstOrNull()?.trim() ?: ""
            if (spoken.isNotBlank()) {
                submitTranscript(spoken)
            }
        } else {
            if (voiceState == VoiceState.LISTENING) {
                voiceState = VoiceState.IDLE
            }
        }
    }

    fun launchSystemVoiceInput() {
        try {
            sttEngine.stopListening()
            errorMessage = ""
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, L10n.lang.toBcp47())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, L10n.lang.toBcp47())
                // FIX 5: deduplicate additional languages
                putExtra(
                    "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                    linkedSetOf(L10n.lang.toBcp47(), "en-IN", "hi-IN").toTypedArray()
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, L10n.t("voice_speak_now"))
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }
            systemSpeechLauncher.launch(intent)
        } catch (e: Throwable) {
            errorMessage = e.message ?: "System voice input unavailable"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            errorMessage = ""
            startListeningInternal(
                sttEngine = sttEngine,
                onStateChange = { voiceState = it },
                onRmsChange = { currentRms = it },
                onTranscriptChange = { transcript = it },
                onFinalResult = { text -> submitTranscript(text) },
                onError = { err ->
                    voiceState = VoiceState.IDLE
                    errorMessage = if (err == "LANGUAGE_UNAVAILABLE") {
                        L10n.t("voice_error_unavailable")
                    } else {
                        err
                    }
                }
            )
        } else {
            voiceState = VoiceState.IDLE
            errorMessage = L10n.t("voice_permission_required")
        }
    }

    fun handleMicTap() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            // FIX 2: block mic tap during THINKING (prevents duplicate concurrent requests)
            if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.THINKING) {
                if (voiceState == VoiceState.LISTENING) {
                    sttEngine.stopListening()
                    voiceState = VoiceState.IDLE
                }
                return
            } else {
                errorMessage = ""
                startListeningInternal(
                    sttEngine = sttEngine,
                    onStateChange = { voiceState = it },
                    onRmsChange = { currentRms = it },
                    onTranscriptChange = { transcript = it },
                    onFinalResult = { text -> submitTranscript(text) },
                    onError = { err ->
                        voiceState = VoiceState.IDLE
                        errorMessage = if (err == "LANGUAGE_UNAVAILABLE") {
                            L10n.t("voice_error_unavailable")
                        } else {
                            err
                        }
                    }
                )
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // FLOATING ACTION BUTTON (Visible on screen when descriptor is present)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 76.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            onClick = {
                isSheetOpen = true
                handleMicTap()
            },
            shape = RoundedCornerShape(28.dp),
            color = DeepNavyBlue,
            shadowElevation = 8.dp,
            border = BorderStroke(1.5.dp, SoftMint)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MicVector(
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = L10n.t("voice_guide_btn"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // MODAL BOTTOM SHEET FOR VOICE GUIDANCE
    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                sttEngine.stopListening()
                TtsManager.stop()
                isSpeaking = false
                isSheetOpen = false
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.t("voice_guide_title"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyBlue
                        )
                        Text(
                            text = descriptor.title,
                            fontSize = 13.sp,
                            color = SubtitleGray
                        )
                    }
                    IconButton(
                        onClick = {
                            sttEngine.stopListening()
                            TtsManager.stop()
                            isSpeaking = false
                            isSheetOpen = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DarkText
                        )
                    }
                }

                // Step Owner Banner if HUMAN or VICTIM_LOCK
                if (descriptor.owner == StepOwner.HUMAN) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = L10n.t("voice_step_owner_human"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else if (descriptor.owner == StepOwner.VICTIM_LOCK) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = L10n.t("voice_step_owner_lock"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Pulsing Mic Animation when LISTENING
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = if (voiceState == VoiceState.LISTENING) 1.25f else 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                // Central Mic Interaction Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val dynamicScale = if (voiceState == VoiceState.LISTENING) {
                        pulseScale + (currentRms.coerceIn(0f, 10f) / 30f)
                    } else 1f

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(dynamicScale)
                            .clip(CircleShape)
                            .background(
                                when (voiceState) {
                                    VoiceState.LISTENING -> WarningAmber
                                    VoiceState.THINKING -> RoyalBlue
                                    VoiceState.GUIDING -> SoftMint
                                    VoiceState.IDLE -> DeepNavyBlue
                                }
                            )
                            .clickable { handleMicTap() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (voiceState == VoiceState.LISTENING) {
                            StopVector(
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            MicVector(
                                tint = if (voiceState == VoiceState.GUIDING) DeepNavyBlue else Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Status text
                Text(
                    text = when (voiceState) {
                        VoiceState.LISTENING -> L10n.t("voice_listening")
                        VoiceState.THINKING -> L10n.t("voice_thinking")
                        VoiceState.GUIDING -> L10n.t("voice_speaking")
                        VoiceState.IDLE -> L10n.t("voice_tap_to_speak")
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SubtitleGray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Dedicated System Voice Input button for 100% device compatibility
                if (voiceState == VoiceState.IDLE) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { launchSystemVoiceInput() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = L10n.t("voice_system_dialog"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = RoyalBlue
                        )
                    }
                }

                // Error / retry banner if speech recognition encountered an issue
                if (errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { handleMicTap() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepNavyBlue)
                                ) {
                                    Text(text = L10n.t("voice_retry"), fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { launchSystemVoiceInput() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyBlue)
                                ) {
                                    Text(text = L10n.t("voice_system_dialog"), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Live transcript card
                if (transcript.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF3F4F6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"$transcript\"",
                            fontSize = 14.sp,
                            color = DarkText,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // AI Guidance Card
                if (currentGuidance.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftMint,
                        border = BorderStroke(1.dp, Color(0xFFB7E4C7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = currentGuidance,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavyBlue,
                                lineHeight = 21.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        if (isSpeaking) {
                                            TtsManager.stop()
                                            isSpeaking = false
                                        } else {
                                            TtsManager.speak(
                                                text = currentGuidance,
                                                languageTag = L10n.lang.toBcp47(),
                                                onStart = { isSpeaking = true },
                                                // FIX 1 (replay button): reset state after TTS done
                                                onDone = {
                                                    isSpeaking = false
                                                    voiceState = VoiceState.IDLE
                                                },
                                                onError = { errMsg ->
                                                    isSpeaking = false
                                                    voiceState = VoiceState.IDLE
                                                    if (errMsg != "TTS_LANG_UNAVAILABLE") {
                                                        errorMessage = errMsg
                                                    }
                                                }
                                            )
                                        }
                                    }
                                ) {
                                    if (isSpeaking) {
                                        StopVector(
                                            tint = DeepNavyBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = DeepNavyBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isSpeaking) L10n.t("voice_stop_speaking") else L10n.t("pg_start"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepNavyBlue
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // FALLBACK TEXT INPUT (Always available so user can type instead of speak)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = typedQuery,
                        onValueChange = { typedQuery = it },
                        placeholder = {
                            Text(
                                text = L10n.t("voice_type_instead"),
                                fontSize = 13.sp,
                                color = SubtitleGray
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            submitTranscript(typedQuery)
                            typedQuery = ""
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .background(DeepNavyBlue, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = L10n.t("voice_send"),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun startListeningInternal(
    sttEngine: SttEngine,
    onStateChange: (VoiceState) -> Unit,
    onRmsChange: (Float) -> Unit,
    onTranscriptChange: (String) -> Unit,
    onFinalResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    onStateChange(VoiceState.LISTENING)
    sttEngine.startListening(
        languageTag = L10n.lang.toBcp47(),
        onReady = {
            onStateChange(VoiceState.LISTENING)
        },
        onRmsChanged = { rms ->
            onRmsChange(rms)
        },
        onPartialResult = { partial ->
            onTranscriptChange(partial)
        },
        onResult = { result ->
            onFinalResult(result)
        },
        onError = { err ->
            onError(err)
        }
    )
}

/** Crisp Canvas microphone vector matching official Material mic iconography */
@Composable
fun MicVector(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Inner mic capsule (top half with rounded corners)
        val capsuleWidth = w * 0.36f
        val capsuleHeight = h * 0.52f
        val capsuleLeft = (w - capsuleWidth) / 2f
        val capsuleTop = h * 0.10f
        drawRoundRect(
            color = tint,
            topLeft = Offset(capsuleLeft, capsuleTop),
            size = Size(capsuleWidth, capsuleHeight),
            cornerRadius = CornerRadius(capsuleWidth / 2f, capsuleWidth / 2f)
        )

        // Outer U-shaped cradle arc
        val arcStroke = w * 0.09f
        val arcTop = h * 0.28f
        val arcHeight = h * 0.42f
        val arcWidth = w * 0.60f
        val arcLeft = (w - arcWidth) / 2f
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(arcLeft, arcTop),
            size = Size(arcWidth, arcHeight),
            style = Stroke(width = arcStroke, cap = StrokeCap.Round)
        )

        // Vertical stem
        drawLine(
            color = tint,
            start = Offset(w / 2f, arcTop + arcHeight),
            end = Offset(w / 2f, h * 0.88f),
            strokeWidth = arcStroke,
            cap = StrokeCap.Round
        )

        // Horizontal base stand
        val baseWidth = w * 0.44f
        drawLine(
            color = tint,
            start = Offset((w - baseWidth) / 2f, h * 0.88f),
            end = Offset((w + baseWidth) / 2f, h * 0.88f),
            strokeWidth = arcStroke,
            cap = StrokeCap.Round
        )
    }
}

/** Crisp Canvas stop vector */
@Composable
fun StopVector(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = minOf(w, h) * 0.68f
        val left = (w - s) / 2f
        val top = (h - s) / 2f
        val radius = s * 0.15f
        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(s, s),
            cornerRadius = CornerRadius(radius, radius)
        )
    }
}
