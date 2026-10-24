package com.cybervictim.assistant.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cybervictim.assistant.ui.screens.*

@Composable
fun AppScaffold(nav: NavHostController) {
    Scaffold(bottomBar = { BottomBar(nav) }) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = androidx.compose.ui.Modifier.padding(padding)) {
            composable(Routes.HOME) { HomeScreen(nav) }
            composable(Routes.GUIDE) { GuideScreen(nav) }
            composable(Routes.EVIDENCE) { EvidenceScreen(nav) }
            composable(Routes.REQUESTS) { RequestsScreen(nav) }
            composable(Routes.MORE) { MoreScreen(nav) }
            composable(Routes.PORTAL_GUIDE) { PortalGuideScreen(nav) }
            composable(Routes.TRACKER) { TrackerScreen(nav) }
            composable(Routes.PHONE_TRACE) { PhoneTraceScreen(nav) }
            composable(Routes.EXPOSURE) { ExposureScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(nav) }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val GUIDE = "guide"
    const val EVIDENCE = "evidence"
    const val REQUESTS = "requests"
    const val MORE = "more"
    const val PORTAL_GUIDE = "portal_guide"
    const val TRACKER = "tracker"
    const val PHONE_TRACE = "phone_trace"
    const val EXPOSURE = "exposure"
    const val SETTINGS = "settings"
}
