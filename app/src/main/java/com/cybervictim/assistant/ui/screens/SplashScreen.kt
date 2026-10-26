package com.cybervictim.assistant.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cybervictim.assistant.R
import com.cybervictim.assistant.ui.AppColors
import com.cybervictim.assistant.ui.Routes
import kotlinx.coroutines.delay

/**
 * First Slide: Official 5-Second Splash Screen
 * Exact 1:1 match with user mockup:
 * - 3D "CB" App Logo
 * - "Cybridge" title
 * - "Report • Get Help • Stay Safe" subtitle
 * - Indian Tricolor wave graphic at bottom
 * - Smooth 5-second progress indicator
 * - Automatically disappears after exactly 5 seconds
 */
@Composable
fun SplashScreen(nav: NavHostController) {
    var animationStarted by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 5000, easing = LinearEasing),
        label = "SplashProgressBar"
    )

    fun proceedToNextSlide() {
        nav.navigate(Routes.LANGUAGE_SELECT) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    LaunchedEffect(Unit) {
        animationStarted = true
        delay(5000L)
        proceedToNextSlide()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.IvoryWhite)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Allow tapping to proceed immediately if victim is in a rush
                proceedToNextSlide()
            }
    ) {
        // Center Content: Logo + Title + Subtitle
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Cybridge Logo",
                modifier = Modifier
                    .size(175.dp)
                    .clip(RoundedCornerShape(38.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Cybridge",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Charcoal,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Report • Get Help • Stay Safe",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.SubtitleCharcoal,
                letterSpacing = 0.5.sp
            )
        }

        // Bottom Section: Tricolor Wave + Progress Indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.tricolor_wave),
                contentDescription = "Indian Tricolor Wave",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.FillWidth
            )

            Spacer(Modifier.height(24.dp))

            // Smooth Progress Indicator (Exact match with user design)
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AppColors.RoyalBlue)
                )
            }

            Spacer(Modifier.height(44.dp))
        }
    }
}
