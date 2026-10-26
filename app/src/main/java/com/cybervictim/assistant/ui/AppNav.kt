package com.cybervictim.assistant.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cybervictim.assistant.ui.screens.*

@Composable
fun AppScaffold(nav: NavHostController) {
    Scaffold { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.SPLASH,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Routes.SPLASH) { SplashScreen(nav) }
            composable(Routes.LANGUAGE_SELECT) { LanguageSelectScreen(nav) }
            composable(Routes.REGISTER_HUB) { RegisterHubScreen(nav) }
            composable(Routes.PORTAL_LOGIN) { PortalLoginScreen(nav) }
            composable(Routes.TELL_US) { TellUsScreen(nav) }
            composable("${Routes.GUIDED_FLOW}/{category}") { backStackEntry ->
                val cat = backStackEntry.arguments?.getString("category") ?: "money"
                GuidedQuestionScreen(nav, cat)
            }
            composable(Routes.EVIDENCE) { EvidenceScreen(nav) }
            composable(Routes.REQUESTS) { RequestsScreen(nav) }
            composable(Routes.PORTAL_GUIDE) { PortalGuideScreen(nav) }
            composable(Routes.PORTAL_WEBVIEW) { PortalWebViewScreen(nav) }
            composable(Routes.PORTAL_CLONE) { PortalCloneScreen(nav) }
            composable("${Routes.PORTAL_CLONE}/{categoryIndex}") { backStackEntry ->
                val catIdx = backStackEntry.arguments?.getString("categoryIndex")?.toIntOrNull() ?: 0
                PortalCloneScreen(nav, catIdx)
            }
            composable(Routes.TRACKER) { TrackerScreen(nav) }
            composable(Routes.PHONE_TRACE) { PhoneTraceScreen(nav) }
            composable(Routes.EXPOSURE) { ExposureScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(nav) }
            composable(Routes.SUSPICIOUS_EMAIL) { SuspiciousEmailScreen(nav) }

            // Legacy fallbacks redirected to RegisterHubScreen
            composable(Routes.HOME) { RegisterHubScreen(nav) }
            composable(Routes.GUIDE) { RegisterHubScreen(nav) }
            composable(Routes.MORE) { RegisterHubScreen(nav) }
        }
    }
}

object Routes {
    const val SPLASH = "splash"
    const val LANGUAGE_SELECT = "language_select"
    const val PORTAL_LOGIN = "portal_login"
    const val TELL_US = "tell_us"
    const val GUIDED_FLOW = "guided_flow"
    const val HOME = "home"
    const val GUIDE = "guide"
    const val EVIDENCE = "evidence"
    const val REQUESTS = "requests"
    const val MORE = "more"
    const val PORTAL_GUIDE = "portal_guide"
    const val PORTAL_WEBVIEW = "portal_webview"
    const val PORTAL_CLONE = "portal_clone"
    const val TRACKER = "tracker"
    const val PHONE_TRACE = "phone_trace"
    const val EXPOSURE = "exposure"
    const val SETTINGS = "settings"
    const val SUSPICIOUS_EMAIL = "suspicious_email"
    const val REGISTER_HUB = "register_hub"
}
