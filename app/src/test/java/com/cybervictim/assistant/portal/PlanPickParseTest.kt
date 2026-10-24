package com.cybervictim.assistant.portal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import org.json.JSONObject

/**
 * Gemini-assisted pick parsing (no network). The model may only SELECT an id;
 * any invented id, malformed JSON, or missing id must fall back to null so the
 * deterministic picker takes over. Clarification questions are surfaced.
 */
class PlanPickParseTest {

    private fun kbJson(): String {
        val candidates = listOf(
            File("../app/src/main/assets/kb/scenario_buttons.json"),
            File("app/src/main/assets/kb/scenario_buttons.json"),
            File("src/main/assets/kb/scenario_buttons.json"),
        )
        return candidates.first { it.exists() }.readText()
    }

    private val kb = JSONObject(kbJson())
    private val scenarios = kb.optJSONArray("scenarios")!!

    @Test
    fun validIdIsAccepted() {
        val pick = PlanBuilder.parsePick(
            "{\"scenario_id\":\"upi_fraud_immediate_transfer\",\"buttons\":null,\"needs_clarification\":\"null\"}",
            scenarios,
        )
        assertEquals("upi_fraud_immediate_transfer", pick.scenarioId)
        assertNull(pick.clarification)
    }

    @Test
    fun wrappedJsonObjectStillParses() {
        val pick = PlanBuilder.parsePick(
            "Here is my decision: {\"scenario_id\":\"fake_job_scam\",\"needs_clarification\":\"Did the job offer ask you to pay anything?\"} thanks",
            scenarios,
        )
        assertEquals("fake_job_scam", pick.scenarioId)
        assertEquals("Did the job offer ask you to pay anything?", pick.clarification)
    }

    @Test
    fun inventedIdIsRejected() {
        val pick = PlanBuilder.parsePick(
            "{\"scenario_id\":\"make_up_new_button_fraud\",\"needs_clarification\":\"null\"}",
            scenarios,
        )
        assertNull("model must never invent categories", pick.scenarioId)
    }

    @Test
    fun malformedJsonIsRejected() {
        assertNull(PlanBuilder.parsePick("not json at all", scenarios).scenarioId)
        assertNull(PlanBuilder.parsePick("<html>error</html>", scenarios).scenarioId)
    }

    @Test
    fun planFromGeminiUsesKbButtonsOnly() {
        val plan = PlanBuilder.planFromGemini(
            kbJson(),
            "{\"scenario_id\":\"hacked_account_email\",\"needs_clarification\":\"null\"}",
        )
        assertTrue(plan.isNotEmpty())
        // strict-flow contract preserved
        assertEquals(StepOwner.VICTIM_LOCK, plan.last().owner)
        assertTrue(plan.any { it.owner == StepOwner.HUMAN && it.narration.contains("credentials") })
        assertTrue(plan.any { it.owner == StepOwner.AI_AUTO && it.narration.contains("Hacking") })
    }

    @Test
    fun clarificationPrependsQuestionStep() {
        val plan = PlanBuilder.planFromGemini(
            kbJson(),
            "{\"scenario_id\":\"sextortion\",\"needs_clarification\":\"Are the photos of you or someone else?\"}",
        )
        assertEquals(StepOwner.HUMAN, plan.first().owner)
        assertTrue(plan.first().narration.contains("Are the photos of you or someone else?"))
        assertEquals(StepOwner.VICTIM_LOCK, plan.last().owner)
    }

    @Test
    fun nullGeminiAnswerYieldsEmptyForFallback() {
        assertTrue(PlanBuilder.planFromGemini(kbJson(), null).isEmpty())
        assertTrue(PlanBuilder.planFromGemini(kbJson(), "").isEmpty())
    }
}
