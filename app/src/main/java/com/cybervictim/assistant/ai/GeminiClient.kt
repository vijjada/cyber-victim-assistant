package com.cybervictim.assistant.ai

import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

/**
 * Gemini-backed AI Case Guide. Uses public REST API with robust fallback.
 * Key comes from BuildConfig manifest placeholder; never stored in code.
 */
object GeminiClient {
    private const val MODEL = "gemini-flash-latest"
    private val MODEL_FALLBACKS = listOf("gemini-flash-latest", "gemini-2.5-flash", "gemini-flash-lite-latest")
    private val apiState = AtomicReference(
        try { com.cybervictim.assistant.BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }
    )

    fun setApiKey(k: String) { apiState.set(k) }

    private val SYSTEM_PROMPT = """
        You are Cybridge: a calm, plain-English helper for cybercrime victims
        in India. Scope is ONLY cyber crime help: guiding the victim, building their case from
        their own evidence, and driving the official cybercrime.gov.in complaint form.
        Rules:
        - Explain every situation in words a non-technical person understands.
        - When uncertain between two portal categories or buttons, ask ONE simple question
          (for example "Did you lose money?") before recommending anything.
        - Never invent portal buttons; you choose only from the provided knowledge base.
        - You may plan AI actions (navigate, select, fill, paste evidence report).
        - Preview and Submit of the final complaint are ALWAYS performed by the victim;
          you must state this whenever the flow reaches that point.
        - Never ask for or store passwords beyond the portal registration the victim enters.
        - Never give hacking, tracing or investigation advice; banks and police take action.
    """.trimIndent()

    /** Reply in the user's chosen app language (Hindi/Telugu/Punjabi/Gujarati/Kannada/Malayalam/Odia/Assamese/etc); English otherwise. */
    private fun languageDirective(): String = when (try {
        com.cybervictim.assistant.ui.L10n.lang.code
    } catch (_: Throwable) { "en" }) {
        "hi" -> "Reply to the user in Hindi (Devanagari, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "te" -> "Reply to the user in Telugu (simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "pa" -> "Reply to the user in Punjabi (Gurmukhi script, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "gu" -> "Reply to the user in Gujarati (Gujarati script, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "mr" -> "Reply to the user in Marathi (Devanagari, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "ta" -> "Reply to the user in Tamil (Tamil script, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "bn" -> "Reply to the user in Bengali (Bengali script, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "kn" -> "Reply to the user in Kannada (Kannada script, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "ml" -> "Reply to the user in Malayalam (Malayalam script, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "or" -> "Reply to the user in Odia (Odia script, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "as" -> "Reply to the user in Assamese (Assamese script, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        else -> ""
    }.let { d -> if (d.isBlank()) "Keep replies in simple English." else d }

    private fun postToMain(action: () -> Unit) {
        try {
            Handler(Looper.getMainLooper()).post(action)
        } catch (_: Throwable) {
            action()
        }
    }

    /** history = (fromUser, text) pairs */
    fun chat(history: List<Pair<Boolean, String>>, userMessage: String, onReply: (String) -> Unit, onError: (String) -> Unit) {
        val key = apiState.get().trim()
        if (key.isEmpty()) {
            postToMain { onError("API key missing — add GEMINI_API_KEY in gradle.properties.") }
            return
        }
        Thread {
            val chain = (listOf(MODEL) + MODEL_FALLBACKS).distinct()
            var lastErr: String? = null

            for (m in chain) {
                try {
                    val contents = org.json.JSONArray()
                    // Filter history to ensure alternating user-model sequence starting with user
                    var expectUser = true
                    for ((isUser, text) in history.dropLast(1)) {
                        if (isUser == expectUser && text.isNotBlank()) {
                            contents.put(turn(isUser, text))
                            expectUser = !expectUser
                        }
                    }
                    contents.put(turn(true, userMessage))

                    val sysInstruction = org.json.JSONObject().put(
                        "parts", org.json.JSONArray().put(
                            org.json.JSONObject().put("text", "$SYSTEM_PROMPT\n\n${languageDirective()}")
                        )
                    )

                    val gen = org.json.JSONObject()
                        .put("temperature", 0.2)
                        .put("maxOutputTokens", 1500)

                    val body = org.json.JSONObject()
                        .put("system_instruction", sysInstruction)
                        .put("contents", contents)
                        .put("generationConfig", gen)
                        .toString()

                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$key")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 4000
                    conn.readTimeout = 6000
                    conn.outputStream.use { it.write(body.toByteArray()) }

                    val status = conn.responseCode
                    if (status in 200..299) {
                        val resp = conn.inputStream.bufferedReader().readText()
                        conn.disconnect()
                        val reply = extractReply(resp)
                        if (reply.isNotBlank()) {
                            postToMain { onReply(reply) }
                            return@Thread
                        }
                    } else {
                        val errStream = conn.errorStream?.bufferedReader()?.readText() ?: ""
                        conn.disconnect()
                        val msg = parseErrorMessage(errStream) ?: "HTTP $status from model $m"
                        lastErr = msg
                    }
                } catch (e: Exception) {
                    lastErr = e.message ?: "network error"
                }
            }

            val finalMsg = lastErr ?: "Unable to get response from AI. Please check your internet connection."
            postToMain { onError(finalMsg) }
        }.start()
    }

    private fun parseErrorMessage(errJson: String): String? {
        return try {
            val root = org.json.JSONObject(errJson)
            root.optJSONObject("error")?.optString("message")?.ifEmpty { null }
        } catch (_: Exception) { null }
    }

    fun currentKey(): String = apiState.get().trim()

    /** Raw response -> text; shared helper. */
    fun extractReplyText(rawJson: String): String = extractReply(rawJson)

    /**
     * One-shot generation for structured tasks (grounded pick, extraction).
     * Returns the text or null on any error; caller falls back deterministically.
     */
    fun generate(prompt: String, maxTokens: Int = 1200): String? {
        return generateWithFallback(prompt, maxTokens)
    }

    /** Tries each available model in order (Free Tier bursts trigger 503s). */
    fun generateWithFallback(prompt: String, maxTokens: Int = 1200): String? {
        val chain = (listOf(MODEL) + MODEL_FALLBACKS).distinct()
        for (m in chain) {
            val out = tryGenerate(m, prompt, maxTokens)
            if (out != null) return out
        }
        return null
    }

    private fun tryGenerate(model: String, prompt: String, maxTokens: Int): String? {
        val key = apiState.get().trim()
        if (key.isEmpty()) return null
        return try {
            val body = org.json.JSONObject()
                .put("contents", org.json.JSONArray().put(
                    org.json.JSONObject().put("role", "user")
                        .put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", prompt)))))
                .put("generationConfig", org.json.JSONObject()
                    .put("temperature", 0.1)
                    .put("maxOutputTokens", maxTokens))
            val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"; conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 4000; conn.readTimeout = 6000
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val status = conn.responseCode
            val resp = if (status in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                ""
            }
            conn.disconnect()
            if (resp.isEmpty()) null else extractReply(resp).ifEmpty { null }
        } catch (_: Exception) { null }
    }

    private fun turn(fromUser: Boolean, text: String): org.json.JSONObject =
        org.json.JSONObject()
            .put("role", if (fromUser) "user" else "model")
            .put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", text)))

    private fun extractReply(json: String): String {
        return try {
            val root = org.json.JSONObject(json)
            val cands = root.optJSONArray("candidates") ?: return ""
            val cand = cands.optJSONObject(0) ?: return ""
            val content = cand.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            buildString {
                for (i in 0 until parts.length()) {
                    val p = parts.optJSONObject(i) ?: continue
                    // Skip thought parts if present
                    if (p.optBoolean("thought", false)) continue
                    p.optString("text")?.let { append(it) }
                }
            }.trim()
        } catch (_: Exception) { "" }
    }
}
