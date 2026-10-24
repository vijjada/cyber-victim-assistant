package com.cybervictim.assistant.remind

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Phase 6 — reminders for the next victim-side steps after submission,
 * plus daily re-checks. Local alarms; no server dependency.
 */
object ReminderScheduler {
    const val CHANNEL = "cva_reminders"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Case reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /** When the victim presses Submit on the portal, schedule the follow-up chain. */
    fun scheduleAfterSubmit(ctx: Context) {
        ensureChannel(ctx)
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val offsets = listOf(
            1L to "Note the acknowledgment number somewhere safe. Check complaint status in the app.",
            3L * 86400_000L to "3-day follow-up: confirm your bank freeze request status.",
            7L * 86400_000L to "1-week follow-up: send the draft follow-up message from the Requests tab.",
            10L * 86400_000L to "10-day exposure check: run the Exposure Warning scan.",
        )
        for ((delay, msg) in offsets) {
            val pi = PendingIntent.getBroadcast(
                ctx, (delay / 1000).toInt(),
                Intent(ctx, ReminderReceiver::class.java).putExtra("msg", msg),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pi)
        }
    }
    const val ACTION_EXPOSURE_RECHECK = "com.cybervictim.assistant.EXPOSURE_RECHECK"
    const val RECHECK_DELAY_MS = 10L * 86400_000L // 10 days

    /**
     * Phase 6-B: recurring 10-day exposure re-check reminder.
     * The alarm reschedules itself each time it fires, so it repeats until
     * the user stops the app data. Exact-alarm APIs when possible, inexact fallback.
     */
    fun scheduleExposureRecheck(ctx: Context) {
        ensureChannel(ctx)
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_EXPOSURE_RECHECK)
            .putExtra("msg", "10-day exposure re-check: verify your email again and re-scan for new breaches.")
        val pi = PendingIntent.getBroadcast(
            ctx, 424242, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = System.currentTimeMillis() + RECHECK_DELAY_MS
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val msg = intent.getStringExtra("msg") ?: "Check your case status."
        if (intent.action == ReminderScheduler.ACTION_EXPOSURE_RECHECK) {
            // keep the 10-day loop going until the user uninstalls or clears data
            ReminderScheduler.scheduleExposureRecheck(ctx)
        }
        val not = androidx.core.app.NotificationCompat.Builder(ctx, ReminderScheduler.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Cyber Victim Assistant")
            .setContentText(msg)
            .setAutoCancel(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(msg.hashCode(), not)
    }
}
