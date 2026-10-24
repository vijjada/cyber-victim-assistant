package com.cybervictim.assistant.portal

import org.json.JSONArray

/** Who is allowed to execute a step on the portal. Enforced everywhere. */
enum class StepOwner { AI_AUTO, HUMAN, VICTIM_LOCK }

/**
 * The single gate of portal automation (v2 rule):
 *  - AI_AUTO: AI taps / fills (navigation, category, fields, evidence report section)
 *  - HUMAN: victim enters credentials, captcha, OTP (never automatable)
 *  - VICTIM_LOCK: final complaint Preview and Submit — the app can NEVER
 *    dispatch these taps. Enforced here and by the caller's UI dialog.
 */
object ActionPolicy {
    private val LOCKED_PREFIXES = listOf(
        "submit", "preview", "final submit",
    )
    private val HUMAN_FIELDS = listOf("captcha", "otp", "otp_verify", "password", "username", "login_id", "mobile_otp", "email_otp")

    fun ownerOf(targetId: String, fieldTag: String? = null): StepOwner {
        val t = targetId.trim().lowercase()
        if (LOCKED_PREFIXES.any { t.startsWith(it) || t == it.trim() }) return StepOwner.VICTIM_LOCK
        if (HUMAN_FIELDS.any { t.contains(it) }) return StepOwner.HUMAN
        return StepOwner.AI_AUTO
    }

    /** Throws if the AI ever tries to dispatch a locked action. */
    fun assertAiAllowed(targetId: String) {
        check(ownerOf(targetId) == StepOwner.AI_AUTO) {
            "BLOCKED: '$targetId' is human-only. Preview/Submit/credentials/captcha/OTP are never AI-dispatched."
        }
    }
}

object PortalDriver {
    data class PortalStep(val owner: StepOwner, val narration: String)

    @Volatile var current: PortalDriver? = null

    fun verifiedDate(): String = try {
        val raw = assetsJson("kb/portal_map.json")
        org.json.JSONObject(raw).optString("lastVerified", "not yet built")
    } catch (_: Exception) { "not yet built" }

    /**
     * Builds the step plan: reads the scenario→button KB for the case, maps
     * each planned action through ActionPolicy, and hands back the approved
     * list. AI actions are executed by the executor; HUMAN/VICTIM_LOCK steps
     * are shown and must be performed by the victim in the app UI.
     */
    fun startCaseFlowFromEvidence(
        caseText: String = "",
        assetsReader: ((String) -> String)? = null,
        onPlan: (List<PortalStep>) -> Unit,
    ) {
        val kbJson = try {
            assetsReader?.invoke("kb/scenario_buttons.json") ?: "{}"
        } catch (_: Exception) { "{}" }
        // Superset ordering happens in the UI; byte-NM keeps guarantee of exactly-one onPlan in each branch.
        fun runKbFallback(plane: (List<PortalStep>) -> Unit) {
            val kbPlan = PlanBuilder.fromKbJson(kbJson, caseText)
            plane(if (kbPlan.scenarioId == null) emptyList() else kbPlan.steps)
        }
        val keyPresent = try {
            com.cybervictim.assistant.ai.GeminiClient.currentKey().isNotEmpty() && caseText.isNotBlank()
        } catch (_: Throwable) { false }
        if (!keyPresent) {
            runKbFallback(onPlan)
            return
        }
        // Grounded Gemini pick on a worker thread; any failure falls back deterministically.
        Thread {
            var handled = false
            try {
                val prompt = com.cybervictim.assistant.ai.KbLoader.buildCasePickerPrompt(caseText)
                val raw = com.cybervictim.assistant.ai.GeminiClient.generateWithFallback(prompt, maxTokens = 700)
                val out = PlanBuilder.planFromGemini(kbJson, raw)
                if (out.isNotEmpty()) {
                    handled = true
                    onPlan(out)
                }
            } catch (_: Throwable) {
                // fall through to deterministic
            }
            if (!handled) runKbFallback(onPlan)
        }.start()
    }

    private fun assetsJson(path: String): String {
        // util wired in Phase 4; safe stub now
        return "{}"
    }
}
