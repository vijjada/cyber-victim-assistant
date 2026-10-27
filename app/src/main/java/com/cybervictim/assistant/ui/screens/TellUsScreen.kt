package com.cybervictim.assistant.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cybervictim.assistant.ai.CaseSessionStore
import com.cybervictim.assistant.ui.L10n
import com.cybervictim.assistant.ui.Routes

private val BgColor = Color(0xFFF7F8F4)
private val TealPrimary = Color(0xFF134E42)
private val TealHalo = Color(0xFFE2EFEA)
private val DarkText = Color(0xFF191C1B)
private val SubtitleGray = Color(0xFF6B7280)
private val BorderColor = Color(0xFFE5E7EB)
private val RecommendedBorder = Color(0xFF059669)
private val RecommendedBg = Color(0xFFECFDF5)

@Composable
fun TellUsScreen(nav: NavHostController) {
    com.cybervictim.assistant.voice.TrackScreenDescriptor(
        com.cybervictim.assistant.voice.ScreenDescriptor(
            screenId = "tell_us",
            title = "Incident Category Selection",
            buttons = listOf("Financial Fraud", "Social Media / Cyber Bullying", "Job Scam", "Hacked Account"),
            fields = emptyList(),
            owner = com.cybervictim.assistant.portal.StepOwner.AI_AUTO,
            hintKey = "voice_hint_tell_us"
        )
    )
    val context = LocalContext.current
    var showTypeDialog by remember { mutableStateOf(false) }
    var typedText by remember { mutableStateOf("") }

    if (showTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = { Text("Tell us what happened", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Describe the incident in your own words. We will ask you step-by-step questions or open the portal clone to register your complaint.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SubtitleGray
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("e.g. I lost Rs 25,000 via UPI to a fraudster who promised a part-time job...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (typedText.isNotBlank()) {
                            CaseSessionStore.current?.onUserMessage(typedText)
                        }
                        showTypeDialog = false
                        nav.navigate(Routes.PORTAL_CLONE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Register on Portal Clone")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTypeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Bar: "Cyber Help" on left, language chip on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cybridge",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.clickable {
                        nav.navigate(Routes.LANGUAGE_SELECT)
                    }
                ) {
                    val langText = if (L10n.lang == L10n.Lang.EN) "English · हिंदी" else "English · ${L10n.lang.label}"
                    Text(
                        text = langText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Heading
            Text(
                text = "Register Your Complaint",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Exact 100% clone buttons from cybercrime.gov.in. Tap below to select your category; the background AI agent silently autofills the official portal on your behalf.",
                fontSize = 14.sp,
                color = SubtitleGray,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary 1-Click Portal Registration Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate(Routes.PORTAL_CLONE) },
                shape = RoundedCornerShape(16.dp),
                color = TealPrimary,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Start Complaint Registration",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Official Portal Clone • Background Auto-Fill",
                            color = Color(0xFFA7F3D0),
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Official Cyber Portal 100% Clone Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECT INCIDENT CATEGORY",
                    fontSize = 12.sp,
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "cybercrime.gov.in",
                    fontSize = 11.sp,
                    color = SubtitleGray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Official Button 1: Online financial fraud (loss of money through UPI, net banking, cards, ATM, etc.)
            ExactPortalCategoryButton(
                title = "Online financial fraud (loss of money through UPI, net banking, cards, ATM, etc.)",
                isAiRecommended = true,
                onClick = { nav.navigate("${Routes.PORTAL_CLONE}/0") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Official Button 2: My phone, computer, email, social media or device was hacked, locked or taken over
            ExactPortalCategoryButton(
                title = "My phone, computer, email, social media or device was hacked, locked or taken over",
                isAiRecommended = false,
                onClick = { nav.navigate("${Routes.PORTAL_CLONE}/1") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Official Button 3: I received fake / threatening calls, or someone is spreading false, hateful or dangerous information online
            ExactPortalCategoryButton(
                title = "I received fake / threatening calls, or someone is spreading false, hateful or dangerous information online",
                isAiRecommended = false,
                onClick = { nav.navigate("${Routes.PORTAL_CLONE}/2") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Official Button 4: A company or organisation's website or system was attacked by a virus, ransomware or malicious software
            ExactPortalCategoryButton(
                title = "A company or organisation's website or system was attacked by a virus, ransomware or malicious software",
                isAiRecommended = false,
                onClick = { nav.navigate("${Routes.PORTAL_CLONE}/3") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Official Button 5: Online gambling, cyber slavery, piracy or deepfakes
            ExactPortalCategoryButton(
                title = "Online gambling, cyber slavery, piracy or deepfakes",
                isAiRecommended = false,
                onClick = { nav.navigate("${Routes.PORTAL_CLONE}/4") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Official Button 6: Something else happened that I do not see above
            ExactPortalCategoryButton(
                title = "Something else happened that I do not see above",
                isAiRecommended = false,
                onClick = { nav.navigate("${Routes.PORTAL_CLONE}/5") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Alternative: 8 Guided Questions Flow
            OutlinedButton(
                onClick = { nav.navigate("${Routes.GUIDED_FLOW}/money") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Text(
                    "Answer Step-by-Step Guided Questions First",
                    color = DarkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Helpline 1930
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val annotatedText = buildAnnotatedString {
                    append("Immediate financial loss? Dial ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))) {
                        append("1930 Helpline")
                    }
                    append(" (Free 24x7)")
                }
                Text(
                    text = annotatedText,
                    fontSize = 13.sp,
                    color = SubtitleGray,
                    modifier = Modifier.clickable {
                        val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                        context.startActivity(dial)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ExactPortalCategoryButton(
    title: String,
    isAiRecommended: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(
            if (isAiRecommended) 1.5.dp else 1.dp,
            if (isAiRecommended) RecommendedBorder else BorderColor
        ),
        shadowElevation = if (isAiRecommended) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (isAiRecommended) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RecommendedBg,
                        border = BorderStroke(1.dp, RecommendedBorder),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            "Recommended Category",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RecommendedBorder,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = DarkText,
                    fontWeight = if (isAiRecommended) FontWeight.SemiBold else FontWeight.Normal,
                    lineHeight = 18.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MicVector(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Draw mic capsule
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.35f, h * 0.12f),
            size = Size(w * 0.30f, h * 0.45f),
            cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
        )
        // Draw U-shape cradle
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.25f),
            size = Size(w * 0.56f, h * 0.40f),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
        // Vertical stem
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.65f),
            end = Offset(w * 0.5f, h * 0.82f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
        // Base plate
        drawLine(
            color = tint,
            start = Offset(w * 0.32f, h * 0.82f),
            end = Offset(w * 0.68f, h * 0.82f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
    }
}
