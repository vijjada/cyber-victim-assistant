package com.cybervictim.assistant.pack

import com.cybervictim.assistant.data.CaseFact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase guaranteed cap: fact budgeting keeps lists bounded before rendering. */
class PackBudgetTest {

    private fun f(i: Int) = CaseFact(
        caseId = "CASE-1", type = "phone", valueText = value999999 + i, sourceEvidenceId = 0,
    )
    private val value999999 = "98123456" // 8 digits; +i keeps values unique

    @Test
    fun emptyBudgetReturnsNothingButCountsAll() {
        val (kept, omitted) = ComplaintPack.budgetFacts(List(10) { f(it) }, 0)
        assertEquals(0, kept.size)
        assertEquals(10, omitted)
    }

    @Test
    fun underBudgetKeepsAll() {
        val (kept, omitted) = ComplaintPack.budgetFacts(List(7) { f(it) }, 2000)
        assertEquals(7, kept.size)
        assertEquals(0, omitted)
    }

    @Test
    fun overBudgetTruncatesAndCounts() {
        val facts = List(2500) { f(it) }
        val (kept, omitted) = ComplaintPack.budgetFacts(facts, 400)
        assertEquals(400, kept.size)
        assertEquals(2100, omitted)
        assertEquals(facts.first().valueText, kept.first().valueText)
        assertEquals(facts[399].valueText, kept.last().valueText)
    }

    @Test
    fun hardCapConstantIsTenMegabytes() {
        assertEquals(10L * 1024 * 1024, ComplaintPack.MAX_BYTES)
        assertTrue(ComplaintPack.TARGET_BYTES < ComplaintPack.MAX_BYTES)
    }
}
