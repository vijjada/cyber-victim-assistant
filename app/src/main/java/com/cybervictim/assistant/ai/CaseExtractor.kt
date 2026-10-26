package com.cybervictim.assistant.ai

import android.os.Handler
import android.os.Looper
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
                val raw = GeminiClient.generateWithFallback(PROMPT + "\n\nEVIDENCE:\n" + textOrOcr, maxTokens = 1200)
                if (raw != null) {
                    val facts = parse(raw)
                    try {
                        Handler(Looper.getMainLooper()).post { onDone(facts) }
                    } catch (_: Throwable) {
                        onDone(facts)
                    }
                } else {
                    try {
                        Handler(Looper.getMainLooper()).post { onError("Could not extract facts from evidence") }
                    } catch (_: Throwable) {
                        onError("Could not extract facts from evidence")
                    }
                }
            } catch (e: Exception) {
                val err = e.message ?: "network error"
                try {
                    Handler(Looper.getMainLooper()).post { onError(err) }
                } catch (_: Throwable) {
                    onError(err)
                }
            }
        }.start()
    }

    fun parse(json: String): List<Fact> = try {
        val first = json.indexOf('{')
        val last = json.lastIndexOf('}')
        val clean = if (first >= 0 && last > first) json.substring(first, last + 1) else json
        val arr = JSONObject(clean).optJSONArray("facts")
        (0 until (arr?.length() ?: 0)).mapNotNull {
            val o = arr!!.getJSONObject(it)
            val t = o.optString("type").trim()
            val v = o.optString("value").trim()
            if (v.isEmpty()) null else Fact(t.ifEmpty { "other" }, v)
        }
    } catch (_: Exception) { emptyList() }
}
