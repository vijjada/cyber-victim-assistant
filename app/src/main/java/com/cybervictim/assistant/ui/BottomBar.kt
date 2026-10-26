package com.cybervictim.assistant.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cybervictim.assistant.ui.Routes

private data class Tab(val route: String, val label: String)

@Composable
fun BottomBar(nav: NavHostController) {
    val tabs = listOf(
        Tab(Routes.HOME, com.cybervictim.assistant.ui.L10n.t("tab_home")),
        Tab(Routes.GUIDE, com.cybervictim.assistant.ui.L10n.t("tab_guide")),
        Tab(Routes.EVIDENCE, com.cybervictim.assistant.ui.L10n.t("tab_evidence")),
        Tab(Routes.REQUESTS, com.cybervictim.assistant.ui.L10n.t("tab_requests")),
        Tab(Routes.MORE, com.cybervictim.assistant.ui.L10n.t("tab_more")),
    )
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    // The "More" tab groups its menu plus the five screens opened from it.
    val moreGroup = setOf(
        Routes.MORE, Routes.PORTAL_GUIDE, Routes.TRACKER,
        Routes.PHONE_TRACE, Routes.EXPOSURE, Routes.SETTINGS,
    )
    NavigationBar(
        modifier = Modifier.height(68.dp),
        containerColor = AppColors.White,
        contentColor = AppColors.Charcoal
    ) {
        tabs.forEach { t ->
            val isSelected = if (t.route == Routes.MORE) current in moreGroup else current == t.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { nav.navigate(t.route) { popUpTo(Routes.HOME) { saveState = true }; launchSingleTop = true; restoreState = true } },
                icon = {},
                label = { Text(t.label, fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.RoyalBlue,
                    selectedTextColor = AppColors.RoyalBlue,
                    indicatorColor = AppColors.SoftMint,
                    unselectedIconColor = AppColors.SubtitleCharcoal,
                    unselectedTextColor = AppColors.SubtitleCharcoal
                )
            )
        }
    }
}
