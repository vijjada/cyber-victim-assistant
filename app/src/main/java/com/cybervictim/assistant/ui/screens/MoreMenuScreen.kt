package com.cybervictim.assistant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cybervictim.assistant.ui.L10n
import com.cybervictim.assistant.ui.Routes

private data class MoreItem(
    val label: String,
    val sub: String,
    val route: String?,
    val built: Boolean = true,
)

/**
 * More menu — the entry hub behind the "More" tab. Each card opens its own
 * screen; unbuilt features show "Coming soon" until their screen exists.
 */
@Composable
fun MoreScreen(nav: androidx.navigation.NavHostController) {
    val items = listOf(
        MoreItem(L10n.t("more_portal"), L10n.t("more_portal_sub"), Routes.PORTAL_GUIDE),
        MoreItem(L10n.t("more_tracker"), L10n.t("more_tracker_sub"), Routes.TRACKER),
        MoreItem(L10n.t("more_phonetrace"), L10n.t("more_phonetrace_sub"), Routes.PHONE_TRACE),
        MoreItem(L10n.t("more_exposure"), L10n.t("more_exposure_sub"), Routes.EXPOSURE),
        MoreItem(L10n.t("more_settings"), L10n.t("more_settings_sub"), Routes.SETTINGS),
    )

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(L10n.t("more_title"), style = MaterialTheme.typography.titleLarge)
        Text(L10n.t("more_subtitle"), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(2.dp))

        items.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = item.built) {
                        if (item.built && item.route != null) {
                            nav.navigate(item.route) { launchSingleTop = true }
                        }
                    },
                colors = if (item.built) CardDefaults.cardColors()
                else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(item.label, style = MaterialTheme.typography.titleMedium)
                    Text(item.sub, style = MaterialTheme.typography.bodySmall)
                    if (!item.built) {
                        Text(
                            L10n.t("coming_soon"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}
