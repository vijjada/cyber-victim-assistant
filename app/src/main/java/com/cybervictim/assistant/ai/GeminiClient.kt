package com.cybervictim.assistant.ai

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

/**
 * Gemini-backed AI Case Guide. Uses gemini-2.0-flash via the public REST API.
 * Key comes from BuildConfig manifest placeholder; never stored in code.
 */
object GeminiClient {
    private const val MODEL = "gemini-3.8-flash"
    private val MODEL_FALLBACKS = listOf("gemini-3.8-flash", "gemini-flash-latest", "gemini-3.1-flash-lite")
    private val apiState = AtomicReference(
        try { com.cybervictim.assistant.BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }
    )

    fun setApiKey(k: String) { apiState.set(k) }

    private val SYSTEM_PROMPT = """
        You are the Cyber Victim Assistant: a calm, plain-English helper for cybercrime victims
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

    /** Reply in the user's chosen app language (Hindi/Telugu); English otherwise. */
    private fun languageDirective(): String = when (try {
        com.cybervictim.assistant.ui.L10n.lang.code
    } catch (_: Throwable) { "en" }) {
        "hi" -> "Reply to the user in Hindi (Devanagari, simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        "te" -> "Reply to the user in Telugu (simple everyday words). Keep UPI IDs, phone numbers, bank names and portal button names exactly as they are."
        else -> ""
    }.let { d -> if (d.isBlank()) "Keep replies in simple English." else d }

    /** history = (fromUser, text) pairs */
    fun chat(history: List<Pair<Boolean, String>>, userMessage: String, onReply: (String) -> Unit, onError: (String) -> Unit) {
        val key = apiState.get().trim()
        if (key.isEmpty()) {
            onError("API key missing — add GEMINI_API_KEY in gradle.properties.")
            return
        }
        Thread {
            try {
                val contents = org.json.JSONArray()
                // system grounding as a leading user/model pair
        contents.put(turn(true, SYSTEM_PROMPT))
        contents.put(turn(true, languageDirective()))
        contents.put(turn(false, "Understood. I will follow these rules."))
                for ((isUser, text) in history.dropLast(1)) { contents.put(turn(isUser, text)) }
                contents.put(turn(true, userMessage))
                val gen = org.json.JSONObject()
                    .put("temperature", 0.2)
                    .put("maxOutputTokens", 1200)
                    .put("thinkingConfig", org.json.JSONObject().put("thinkingBudget", 0))
                val body = org.json.JSONObject().put("contents", contents).put("generationConfig", gen).toString()
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$key")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 20000
                conn.readTimeout = 45000
                conn.outputStream.use { it.write(body.toByteArray()) }
                val resp = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val reply = extractReply(resp)
                onReply(reply.ifEmpty { "I did not get a clear answer. Please describe the incident once more." })
            } catch (e: Exception) {
                onError(e.message ?: "network error")
            }
        }.start()
    }

    fun currentKey(): String = apiState.get().trim()

    /** Raw response -> text; shared helper. */
    fun extractReplyText(rawJson: String): String = extractReply(rawJson)

    /**
     * One-shot generation for structured tasks (grounded pick, extraction).
     * Returns the text or null on any error; caller falls back deterministically.
     */
    fun generate(prompt: String, maxTokens: Int = 1200): String? {
        val key = apiState.get().trim()
        if (key.isEmpty()) return null
        return try {
            val body = org.json.JSONObject()
                .put("contents", org.json.JSONArray().put(
                    org.json.JSONObject().put("role", "user")
                        .put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", prompt)))))
                .put("generationConfig", org.json.JSONObject()
                    .put("temperature", 0.1)
                    .put("maxOutputTokens", maxTokens)
                    .put("thinkingConfig", org.json.JSONObject().put("thinkingBudget", 0)))
            val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$key")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"; conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 20000; conn.readTimeout = 60000
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val resp = try { conn.inputStream.bufferedReader().readText() } catch (_: Exception) { "" }
            conn.disconnect()
            if (resp.isEmpty()) null else extractReply(resp).ifEmpty { null }
        } catch (_: Exception) { null }
    }

    /** Tries each available model in order (Free Tier bursts trigger 503s). */
    fun generateWithFallback(prompt: String, maxTokens: Int = 700): String? {
        val chain = listOf(MODEL) + MODEL_FALLBACKS
        for (m in chain.distinct()) {
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
                    .put("maxOutputTokens", maxTokens)
                    .put("thinkingConfig", org.json.JSONObject().put("thinkingBudget", 0)))
            val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"; conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000; conn.readTimeout = 45000
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val resp = try { conn.inputStream.bufferedReader().readText() } catch (_: Exception) { "" }
            conn.disconnect()
            if (resp.isEmpty()) null else extractReply(resp).ifEmpty { null }
        } catch (_: Exception) { null }
    }

    private fun turn(fromUser: Boolean, text: String): org.json.JSONObject =
        org.json.JSONObject()
            .put("role", if (fromUser) "user" else "model")
            .put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", text)))

    private fun extractReply(json: String): String {
        // minimal parse of candidates[0].content.parts[].text (org.json is bundled on Android)
        return try {
            val root = org.json.JSONObject(json)
            val cands = root.getJSONArray("candidates")
            val parts = cands.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
            buildString { for (i in 0 until parts.length()) parts.getJSONObject(i).optString("text")?.let { append(it) } }
        } catch (_: Exception) { "" }
    }

    private fun j(s: String): String = org.json.JSONObject.quote(s)
}
