package com.cybervictim.assistant.ai

import org.json.JSONObject

/**
 * Gemini multimodal fact extraction for Case Builder evidence.
 * Runs in a worker thread; never uploads the file itself — see docs.
 */
object CaseExtractor {
    data class Fact(val type: String, val value: String)

    private const val PROMPT = """
        From this evidence (screenshot / SMS text / bank statement lines), extract
        facts useful for a cybercrime complaint: UPI IDs, phone numbers,
        transaction references (txn/UTR), amounts, timestamps, names of portals
        or apps. Respond ONLY as JSON:
        {"facts":[{"type":"upi_id|phone|txn_ref|amount|timestamp|app|other","value":"..."}]}
        No commentary.
    """

    fun extract(
        textOrOcr: String,
        onDone: (List<Fact>) -> Unit,
        onError: (String) -> Unit,
    ) {
        val key = GeminiClient.currentKey()
        if (key.isEmpty()) { onError("API key missing (gradle.properties GEMINI_API_KEY)"); return }
        Thread {
            try {
                val body = JSONObject()
                    .put("contents", org.json.JSONArray().put(
                        JSONObject().put("role", "user").put("parts",
                            org.json.JSONArray().put(JSONObject().put("text", PROMPT + "\n\nEVIDENCE:\n" + textOrOcr))
                        )
                    ))
                    .put("generationConfig", JSONObject().put("temperature", 0.1).put("maxOutputTokens", 900))
                val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$key")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 20000; conn.readTimeout = 60000
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val resp = conn.inputStream.bufferedReader().readText(); conn.disconnect()
                val facts = parse(GeminiClient.extractReplyText(resp))
                onDone(facts)
            } catch (e: Exception) { onError(e.message ?: "network error") }
        }.start()
    }

    fun parse(json: String): List<Fact> = try {
        val arr = JSONObject(json).optJSONArray("facts")
        (0 until (arr?.length() ?: 0)).mapNotNull {
            val o = arr!!.getJSONObject(it)
            val t = o.optString("type").trim()
            val v = o.optString("value").trim()
            if (v.isEmpty()) null else Fact(t.ifEmpty { "other" }, v)
        }
    } catch (_: Exception) { emptyList() }
}
