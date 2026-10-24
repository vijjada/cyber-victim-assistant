package com.cybervictim.assistant.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads the portal knowledge base (grounded assets) and asks Gemini, via
 * GeminiClient.chatJson, which scenario matches the victim's case text.
 * The AI picks ONLY from the KB scenarios; if unsure it asks back.
 */
object KbLoader {
    var assets: ((String) -> String)? = null // set by MainActivity: (path) -> String

    fun load(name: String): JSONObject? = try {
        JSONObject(assets?.invoke("kb/$name") ?: "{}")
    } catch (_: Exception) { null }

    fun buildCasePickerPrompt(caseText: String): String {
        val kb = load("scenario_buttons.json")
        val scenarios = kb?.optJSONArray("scenarios") ?: JSONArray()
        return """
            A cybercrime victim describes their case. Pick the matching scenario
            from the knowledge base and list the exact portal buttons in order.
            If two scenarios could fit, ask ONE short question instead of guessing.

            CASE TEXT:
            $caseText

            KNOWLEDGE BASE SCENARIOS (pick strictly from these ids):
            $scenarios

            Respond as JSON: {"scenario_id":"...", "buttons":[...], "needs_clarification": "question or null"}
        """.trimIndent()
    }
}
