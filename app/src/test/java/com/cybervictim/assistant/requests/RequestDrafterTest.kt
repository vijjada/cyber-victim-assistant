package com.cybervictim.assistant.requests

import com.cybervictim.assistant.data.CaseFact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 6-D: draft templating builds on real facts and keeps placeholders empty. */
class RequestDrafterTest {

    private val facts = listOf(
        CaseFact(caseId = "CASE-1", type = "upi_id", valueText = "fraud@ybl", sourceEvidenceId = 0),
        CaseFact(caseId = "CASE-1", type = "phone", valueText = "9812345678", sourceEvidenceId = 0),
        CaseFact(caseId = "CASE-1", type = "txn_ref", valueText = "UTR12345678", sourceEvidenceId = 0),
        CaseFact(caseId = "CASE-1", type = "amount", valueText = "₹24,500", sourceEvidenceId = 0),
        CaseFact(caseId = "CASE-1", type = "timestamp", valueText = "12/10/2026 21:14", sourceEvidenceId = 0),
    )

    @Test
    fun freezeUsesRealFacts() {
        val d = RequestDrafter.draft(RequestDrafter.FREEZE, "CASE-1", facts)
        assertTrue(d.contains("UTR12345678"))
        assertTrue(d.contains("fraud@ybl"))
        assertTrue(d.contains("9812345678"))
        assertTrue(d.contains("₹24,500"))
        assertTrue(d.contains("FREEZE the recipient account"))
        assertFalse(d.contains("<UPI ID>"))
    }

    @Test
    fun missingFactsFallBackToPlaceholders() {
        val d = RequestDrafter.draft(RequestDrafter.FREEZE, "CASE-1", emptyList())
        assertTrue(d.contains("<UPI ID>"))
        assertTrue(d.contains("<amount>"))
        assertTrue(d.contains("<transaction reference>"))
    }

    @Test
    fun followupReferencesCaseId() {
        val d = RequestDrafter.draft(RequestDrafter.FOLLOWUP, "ACK-99", emptyList())
        assertTrue(d.contains("complaint ACK-99"))
    }

    @Test
    fun docsListsPowerOfAttorneyElseCheckingList() {
        val d = RequestDrafter.draft(RequestDrafter.DOCS, "CASE-1", facts)
        assertTrue(d.contains("Complaint acknowledgment number"))
        assertTrue(d.contains("Aadhaar/PAN"))
        assertTrue(d.contains("fraud@ybl"))
    }

    @Test
    fun unfreezeNeverLeadsWithGuilt() {
        val d = RequestDrafter.draft(RequestDrafter.UNFREEZE, "CASE-1", facts)
        assertTrue(d.startsWith("Subject:"))
        assertTrue(d.contains("unfreezing of my"))
    }
}
