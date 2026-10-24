package com.cybervictim.assistant.portal

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds the portal step plan from the grounded KB (scenario_buttons.json).
 * Deterministic on-device picker: curated per-scenario keywords, word-boundary
 * matching, argmax + margin; when unequal candidates tie, it asks back
 * (the PDF "asks when unsure" rule). Every step passes ActionPolicy.
 */
object PlanBuilder {

    data class ApprovedPlan(
        val scenarioId: String?,
        val clarification: String?,
        val steps: List<PortalDriver.PortalStep>,
    )

    fun fromKbJson(kbJson: String, caseText: String): ApprovedPlan {
        val kb = try { JSONObject(kbJson) } catch (_: Exception) {
            return ApprovedPlan(null, "Did you lose money?", emptyList())
        }
        val scenarios = kb.optJSONArray("scenarios") ?: JSONArray()
        val text = caseText.lowercase()
        val words = text.split(Regex("[^a-z0-9@]+")).filter { it.length > 2 }.toSet()

        data class Scored(val s: JSONObject, val score: Int)
        val scored = (0 until scenarios.length()).map { i ->
            val s = scenarios.getJSONObject(i)
            Scored(s, matchScore(text, words, s))
        }.sortedByDescending { it.score }

        val top = scored.firstOrNull() ?: return ApprovedPlan(null, "Tell me what happened in one line.", emptyList())
        val second = scored.getOrNull(1)

        return when {
            top.score <= 0 -> ApprovedPlan(
                null,
                "Did you lose money, and was it a person online or an app/website? (needed to pick the right category)",
                emptyList(),
            )
            second != null && top.score - second.score < 1 -> ApprovedPlan(
                top.s.optString("id"),
                "Was this about money transfer, a job/task, or someone hacking your account? (case is ambiguous)",
                buildSequence(top.s),
            )
            else -> ApprovedPlan(
                top.s.optString("id"),
                null,
                buildSequence(top.s),
            )
        }
    }

    private fun matchScore(text: String, words: Set<String>, scenario: JSONObject): Int {
        val kws = scenario.optJSONArray("keywords") ?: return 0
        var score = 0
        for (i in 0 until kws.length()) {
            val k = kws.optString(i).lowercase().trim()
            if (k.isEmpty() || k.contains("?") || k.endsWith("? none")) continue
            if (k.contains(" ")) { if (text.contains(k)) score += 3 }
            else {
                for (w in words) {
                    val exact = w == k
                    val prefix = k.length >= 4 && w.length >= k.length && (w.startsWith(k) || w.endsWith(k))
                    if (exact || prefix) { score += (if (exact) 2 else 1); break }
                }
            }
        }
        return score
    }

    /**
     * Official portal sequence (MHA/I4C citizen manual, §§ Step 1–7), grounded
     * on the archived manual + live portal crawl. AI handles navigation and
     * form filling; credentials, security answer, OTP, ID uploads, Preview and
     * Submit stay with the victim (ActionPolicy-enforced, see LOCKED/HUMAN).
     */
    fun buildSequence(scenario: JSONObject): List<PortalDriver.PortalStep> {
        val list = mutableListOf(
            PortalDriver.PortalStep(StepOwner.AI_AUTO, "Step 1 — Open https://cybercrime.gov.in and click 'File a complaint'.")
        )
        val buttons = scenario.optJSONArray("buttons") ?: JSONArray()
        for (i in 0 until buttons.length()) {
            val b = buttons.optString(i)
            if (b.isNotBlank() && !b.contains("the sub-category", ignoreCase = true) &&
                !b.contains("route", ignoreCase = true) && !b.contains("section", ignoreCase = true)) {
                list.add(PortalDriver.PortalStep(StepOwner.AI_AUTO, "Tap '$b' because it matches your case type."))
            }
        }
        list.add(PortalDriver.PortalStep(StepOwner.HUMAN, "Tick the 'I Accept' acknowledgement checkbox — this consent declaration is yours."))
        list.add(1, PortalDriver.PortalStep(StepOwner.AI_AUTO, "Click 'Report Other Cyber Crime'."))
        list.add(PortalDriver.PortalStep(StepOwner.HUMAN,
            "credentials: enter 'User Name' and your Mobile Number, press 'Get OTP', type the OTP (valid 30 min), answer the security question and press Submit — the app never performs these."))
        val cat = scenario.optString("official_category")
        val sub = scenario.optString("official_sub_category")
        if (cat.isNotBlank()) list.add(PortalDriver.PortalStep(StepOwner.AI_AUTO, "Category of complaint: select '$cat' (Mandatory)."))
        if (sub.isNotBlank()) list.add(PortalDriver.PortalStep(StepOwner.AI_AUTO, "Sub-category of Crime: select '$sub' (Mandatory)."))
        list.add(PortalDriver.PortalStep(StepOwner.AI_AUTO,
            "Victim Verification Login: Enter your name and mobile number, press 'Get OTP', enter the OTP received (valid 30 minutes), answer the security question for authentication, then press Submit."))
        val fields = scenario.optJSONArray("portal_fields") ?: JSONArray()
        if (fields.length() > 0) {
            val sb = StringBuilder()
            for (i in 0 until fields.length()) {
                if (i > 0) sb.append("; ")
                sb.append(fields.optString(i))
            }
            list.add(PortalDriver.PortalStep(StepOwner.AI_AUTO,
                "Incident Details — fill from your Case Builder facts: $sb. Evidence uploads must be ≤5 MB each."))
        }
        list.add(PortalDriver.PortalStep(StepOwner.HUMAN,
            "Complainant Details — your personal data is NOT in the case facts: Gender/DOB, Father/Mother/Spouse name (Mandatory), address; upload your National ID (Voter/PAN/Driving/Govt. card) yourself (Mandatory)."))
        list.add(PortalDriver.PortalStep(StepOwner.AI_AUTO,
            "Suspect Details — fill name/IDs/number from your facts if available; press 'Add More' in chat to add another suspect."))
        list.add(PortalDriver.PortalStep(StepOwner.AI_AUTO,
            "Navigate with 'Save and Next' through the tabs, then press 'Save & Preview'."))
        list.add(PortalDriver.PortalStep(StepOwner.VICTIM_LOCK,
            "PREVIEW — the AI has done a read-only consistency check; press Preview yourself."))
        list.add(PortalDriver.PortalStep(StepOwner.VICTIM_LOCK,
            "SUBMIT — press Submit — this tap is yours alone. Note the Complaint ID, then use 'Download PDF' to keep a record."))
        return list
    }

    /** Back-compat path: generic sequence for raw button arrays (no scenario object). */
    fun buildSequence(buttons: JSONArray): List<PortalDriver.PortalStep> {
        val fake = JSONObject().put("buttons", buttons)
        return buildSequence(fake)
    }


    /**------ Gemini-assisted pick (grounded: the model only selects an id) ------*/

    data class Pick(val scenarioId: String?, val clarification: String?)

    /**
     * Parses the model's JSON answer. Returns a valid scenarioId only if it
     * exists in the KB — buttons always come from the KB entry, never the model.
     */
    fun parsePick(raw: String, scenarios: JSONArray): Pick {
        val first = raw.indexOf('{')
        val last = raw.lastIndexOf('}')
        if (first < 0 || last <= first) return Pick(null, null)
        val id: String
        val q: String?
        try {
            val o = JSONObject(raw.substring(first, last + 1))
            id = o.optString("scenario_id").trim()
            val need = o.optString("needs_clarification").trim()
            q = if (need.isEmpty() || need == "null") null else need
        } catch (_: Exception) { return Pick(null, null) }
        var found = false
        for (i in 0 until scenarios.length()) {
            if (scenarios.getJSONObject(i).optString("id") == id) { found = true; break }
        }
        return Pick(if (found) id else null, q)
    }

    fun scenarioById(kbJson: String, scenarioId: String?): JSONObject? {
        if (scenarioId == null) return null
        val scenarios = try { JSONObject(kbJson).optJSONArray("scenarios") } catch (_: Exception) { null } ?: return null
        for (i in 0 until scenarios.length()) {
            val s = scenarios.getJSONObject(i)
            if (s.optString("id") == scenarioId) return s
        }
        return null
    }

    fun scenarioButtons(kbJson: String, scenarioId: String?): JSONArray? =
        scenarioById(kbJson, scenarioId)?.optJSONArray("buttons")

    fun planForPick(kbJson: String, pick: Pick): List<PortalDriver.PortalStep> {
        val sc = scenarioById(kbJson, pick.scenarioId) ?: return emptyList()
        val steps = buildSequence(sc)
        if (pick.clarification != null) {
            return listOf(
                PortalDriver.PortalStep(StepOwner.HUMAN,
                    "The AI wants one answer before it proceeds (reply in the Guide chat): " + pick.clarification)
            ) + steps
        }
        return steps
    }

    /** Full pick path: Gemini raw answer -> validated plan; empty on any invalid input (caller falls back). */
    fun planFromGemini(kbJson: String, geminiRaw: String?): List<PortalDriver.PortalStep> {
        if (geminiRaw.isNullOrBlank()) return emptyList()
        val scenarios = try { JSONObject(kbJson).optJSONArray("scenarios") } catch (_: Exception) { null }
            ?: return emptyList()
        val pick = parsePick(geminiRaw, scenarios)
        return if (pick.scenarioId != null) planForPick(kbJson, pick) else emptyList()
    }
}
