package com.cybervictim.assistant.trc

import android.content.Context
import android.os.Build

/**
 * Phase 7 — PhoneTrace: how did the attacker likely get in?
 * Reads only normal Android APIs; never uses the Accessibility permission itself.
 * Output wording is always "suspicious"/"likely", never "proven culprit".
 */
data class AppRisk(
    val pkg: String,
    val label: String,
    val firstInstall: Long,
    val riskyAccess: List<String>,
    val score: Int,
)

object PhoneTrace {

    private val REMOTE_TOOLS =
        listOf("anydesk", "teamviewer", "rustdesk", "airdroid", "vnc", "rdp", "scrcpy", "quicksupport", "atservice")

    /** Packages currently holding Accessibility service (read-only evaluation). */
    fun accessibilityEnabledPkgs(ctx: Context): List<String> {
        val enabled = android.provider.Settings.Secure.getString(
            ctx.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return emptyList()
        return enabled.split(":").filter { it.isNotBlank() }
            .map { it.substringBefore("/") }
    }

    fun scan(ctx: Context, fraudTime: Long): List<AppRisk> {
        val pm = ctx.packageManager
        val acc = accessibilityEnabledPkgs(ctx).toSet()
        val out = mutableListOf<AppRisk>()

        for (pi in pm.getInstalledPackages(PackageManager_GET_PERMISSIONS)) {
            val ai = pi.applicationInfo ?: continue
            if (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0) continue
            val label = try { pm.getApplicationLabel(ai).toString() } catch (_: Exception) { pi.packageName }
            val risky = mutableListOf<String>()

            if (pi.packageName in acc) risky.add("accessibility")
            val perms = pi.requestedPermissions ?: emptyArray()
            if (perms.any { it == "android.permission.SYSTEM_ALERT_WINDOW" }) risky.add("overlay")
            if (perms.any { it == "android.permission.RECEIVE_SMS" || it == "android.permission.READ_SMS" }) risky.add("sms-read")

            val fromPlay = if (Build.VERSION.SDK_INT >= 30) {
                try { pm.getInstallSourceInfo(pi.packageName).installingPackageName == "com.android.vending" } catch (_: Exception) { true }
            } else true
            if (!fromPlay) risky.add("sideloaded")

            val recent = fraudTime > 0 && pi.firstInstallTime in (fraudTime - 30L * 86400_000L)..(fraudTime + 48L * 3600_000L)
            if (recent) risky.add("installed-near-fraud-time")

            val lower = (pi.packageName + " " + label).lowercase()
            if (REMOTE_TOOLS.any { it in lower }) risky.add("remote-control-tool")

            if (risky.isEmpty()) continue
            out.add(AppRisk(pi.packageName, label, pi.firstInstallTime, risky, PhoneScore.score(risky)))
        }
        return out.sortedByDescending { it.score }
    }

    private val PackageManager_GET_PERMISSIONS =
        android.content.pm.PackageManager.GET_PERMISSIONS

    /**------ Usage timeline (needs the victim's own one-time special grant) ------*/
    fun usageAccessGranted(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = am.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), ctx.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessIntent(ctx: Context): android.content.Intent =
        android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Apps actually opened around the fraud time (ACTIVITY_RESUMED events). */
    fun usageOpenedAround(ctx: Context, pivot: Long, windowMs: Long = 48L * 3600_000L): Map<String, Int> {
        if (!usageAccessGranted(ctx)) return emptyMap()
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val from = pivot - windowMs; val to = pivot + windowMs
        val counts = HashMap<String, Int>()
        val events = usm.queryEvents(from, to)
        val ev = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            if (ev.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                counts[ev.packageName] = (counts[ev.packageName] ?: 0) + 1
            }
        }
        return counts
    }

    /**------ Weekly baseline: snapshots installed apps so new/removed/changed shows ------*/
    private const val PREFS = "phonetrace_baseline"
    private const val KEY = "snapshot"

    fun saveBaseline(ctx: Context, risks: List<AppRisk>, all: List<android.content.pm.PackageInfo>) {
        val json = org.json.JSONArray()
        for (pi in all) {
            val ai = pi.applicationInfo ?: continue
            if (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0) continue
            json.put(org.json.JSONObject().put("pkg", pi.packageName).put("install", pi.firstInstallTime))
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.toString())
            .putLong("saved_at", System.currentTimeMillis()).apply()
    }

    fun compareBaseline(ctx: Context): String? {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        val old = org.json.JSONArray(raw)
        val prev = HashMap<String, Long>()
        for (i in 0 until old.length()) {
            val obj = old.optJSONObject(i) ?: continue
            val pkg = obj.optString("pkg")
            if (pkg.isNotEmpty()) {
                prev[pkg] = obj.optLong("install")
            }
        }
        val pm = ctx.packageManager
        val now = HashMap<String, Long>()
        for (pi in pm.getInstalledPackages(0)) {
            val ai = pi.applicationInfo ?: continue
            if (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0) continue
            now[pi.packageName] = pi.firstInstallTime
        }
        val newApps = now.keys - prev.keys
        val removed = prev.keys - now.keys
        val changed = now.keys.intersect(prev.keys).filter { now[it] != prev[it] }
        val at = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("saved_at", 0L)
        val fmt = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.US)
        return buildString {
            append("Baseline from ").append(fmt.format(java.util.Date(at)))
            append(" — New: ").append(newApps.take(6).joinToString().ifEmpty { "none" })
            append(" • Removed: ").append(removed.take(6).joinToString().ifEmpty { "none" })
            append(" • Changed: ").append(changed.take(6).joinToString().ifEmpty { "none" })
        }
    }
}
