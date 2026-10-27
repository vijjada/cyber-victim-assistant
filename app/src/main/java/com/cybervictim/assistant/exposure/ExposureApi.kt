package com.cybervictim.assistant.exposure

import java.net.HttpURLConnection
import java.net.URL
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.json.JSONArray

/**
 * Phase 8 client for the exposure backend (tools/backend).
 * Endpoint via EXPOSURE_BASE_URL gradle property; empty = "not linked yet",
 * UI gracefully stays on the password-exposure check. Worker threads only.
 */
object ExposureApi {
    private val mainHandler = Handler(Looper.getMainLooper())

    data class ApiRes(val ok: Boolean, val message: String, val breaches: List<JSONObject> = emptyList())

    // Phase 9-B: token is Keystore-encrypted at rest; init() restores it.
    @Volatile private var appCtx: android.content.Context? = null
    var token: String? = null
        private set

    fun init(c: android.content.Context) {
        appCtx = c.applicationContext
        token = SecPrefs.get(c).getString("exposure_token", null)
    }

    private fun rememberToken(t: String) {
        token = t
        appCtx?.let { SecPrefs.get(it).edit().putString("exposure_token", t).apply() }
    }

    private fun forgetToken() {
        token = null
        appCtx?.let { SecPrefs.get(it).edit().remove("exposure_token").apply() }
    }

    fun baseUrl(): String = try {
        com.cybervictim.assistant.BuildConfig.EXPOSURE_BASE_URL.trim().trimEnd('/')
    } catch (_: Throwable) {
        ""
    }

    fun isLinked(): Boolean = baseUrl().isNotEmpty()

    private fun exchange(method: String, path: String, bodyOrNull: String?): String {
        val conn = URL(baseUrl() + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        if (bodyOrNull != null) conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.setRequestProperty("Content-Type", "application/json")
        if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token)
        if (bodyOrNull != null) conn.outputStream.use { it.write(bodyOrNull.toByteArray()) }
        val txt = try { conn.inputStream.bufferedReader().readText() } catch (_: Exception) { "{}" }
        return "" + conn.responseCode + "|" + txt
    }

    private fun parseBody(s: String): JSONObject {
        return try {
            val idx = s.indexOf('|')
            JSONObject(if (idx >= 0) s.substring(idx + 1) else "{}")
        } catch (_: Exception) { JSONObject() }
    }

    private fun codeOf(s: String): Int {
        return try { s.substring(0, s.indexOf('|')).toInt() } catch (_: Exception) { 0 }
    }

    fun startVerify(email: String, onDone: (String) -> Unit) {
        val safeDone = { msg: String -> mainHandler.post { onDone(msg) } }
        Thread {
            val base = baseUrl()
            if (base.isEmpty()) {
                safeDone("Backend not linked yet. (Set EXPOSURE_BASE_URL after deploying tools/backend.)")
                return@Thread
            }
            val e = email.trim().lowercase()
            if (!e.contains("@")) {
                safeDone("Enter a valid email.")
                return@Thread
            }
            val resp = try {
                exchange("POST", "/verify-email/start", JSONObject().put("email", e).toString())
            } catch (ex: Exception) {
                safeDone("Network problem: " + (ex.message ?: "offline"))
                return@Thread
            }
            val body = parseBody(resp)
            val rc = codeOf(resp)
            when {
                rc == 200 -> safeDone("Check the inbox for a 6-digit code")
                rc == 503 -> safeDone("Email sender not configured on server. " + body.optString("hint"))
                else -> safeDone(body.optString("error", "server " + rc))
            }
        }.start()
    }

    fun checkCode(email: String, code: String, onDone: (String) -> Unit) {
        val safeDone = { msg: String -> mainHandler.post { onDone(msg) } }
        Thread {
            val resp = try {
                exchange("POST", "/verify-email/check",
                    JSONObject().put("email", email.trim().lowercase()).put("code", code.trim()).toString())
            } catch (ex: Exception) { safeDone("Network problem: " + (ex.message ?: "offline")); return@Thread }
            val rc = codeOf(resp)
            val body = parseBody(resp)
            if (rc == 200 && body.optBoolean("ok")) {
                rememberToken(body.optString("token"))
                safeDone("Email verified")
            } else safeDone(body.optString("error", "server " + rc))
        }.start()
    }

    fun scanBreaches(email: String, onDone: (ApiRes) -> Unit) {
        val safeDone = { res: ApiRes -> mainHandler.post { onDone(res) } }
        Thread {
            val t = token
            if (t == null) { safeDone(ApiRes(false, "Verify your email first.")); return@Thread }
            val resp = try {
                exchange("GET", "/breaches/" + email.trim().lowercase(), null)
            } catch (ex: Exception) { safeDone(ApiRes(false, "Network problem: " + (ex.message ?: "offline"))); return@Thread }
            val rc = codeOf(resp)
            val body = parseBody(resp)
            if (rc == 401) { forgetToken(); safeDone(ApiRes(false, "Session expired — verify again.")); return@Thread }
            if (rc == 404) { safeDone(ApiRes(true, "not-found")); return@Thread }
            if (rc !in 200..299) { safeDone(ApiRes(false, body.optString("error", "server " + rc))); return@Thread }
            if (!body.optBoolean("configured", true)) {
                safeDone(ApiRes(false, "Server has no breach-source key yet. " + body.optString("note")))
                return@Thread
            }
            val arr = body.optJSONArray("breaches") ?: JSONArray()
            val list = ArrayList<JSONObject>()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { list.add(it) }
            }
            safeDone(ApiRes(true, "", list))
        }.start()
    }
}
