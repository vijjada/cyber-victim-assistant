package com.cybervictim.assistant.exposure

import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Phase 8 — 10-Day Exposure Warning (privacy-safe k-anonymity password check).
 * Only the first 5 hex chars of the SHA-1 hash EVER leave the phone.
 * The full password is never stored or transmitted. Full email-breach lookup
 * lands later with the Firebase verification-code backend.
 */
object ExposureChecker {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun passwordExposed(password: String, cb: (exposed: Boolean, count: Int) -> Unit, fail: (String) -> Unit) {
        val safeCb = { exposed: Boolean, count: Int -> mainHandler.post { cb(exposed, count) } }
        val safeFail = { msg: String -> mainHandler.post { fail(msg) } }
        Thread {
            try {
                val sha1 = MessageDigest.getInstance("SHA-1").digest(password.toByteArray())
                    .joinToString("") { "%02X".format(it) }
                val prefix = sha1.substring(0, 5)
                val suffix = sha1.substring(5)
                val conn = URL("https://api.pwnedpasswords.com/range/$prefix").openConnection() as HttpURLConnection
                conn.connectTimeout = 15000; conn.readTimeout = 30000
                conn.requestMethod = "GET"
                conn.setRequestProperty("Add-Padding", "true")
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                var exposed = false
                var count = 0
                for (line in body.lineIterator()) {
                    val parts = line.split(":")
                    if (parts.size == 2 && parts[0].trim().equals(suffix, ignoreCase = true)) {
                        exposed = true; count = parts[1].trim().toIntOrNull() ?: 0
                    }
                }
                safeCb(exposed, count)
            } catch (e: Exception) { safeFail(e.message ?: "network error") }
        }.start()
    }

    private fun String.lineIterator() = this.split("\r\n", "\n").filter { it.isNotBlank() }.iterator()
}
