package com.cybervictim.assistant.pack

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Canvas
import java.io.File

/**
 * Phase 3 completion — builds the bank/cyber-cell complaint pack as a real
 * PDF from the victim's on-device facts, with every fact traced to its
 * evidence item. Prepared on-device; the victim shares/submits it.
 */
object ComplaintPack {

    /** HARD CAP: the pack must never exceed 10 MB on disk. */
    const val MAX_BYTES = 10L * 1024 * 1024
    /** Target under which we accept the full pack. */
    const val TARGET_BYTES = 9L * 1024 * 1024

    /**
     * Pure budgeting for the facts section: keep the first [max] facts and
     * count the remainder so the note can say how many are stored on-device.
     */
    fun budgetFacts(facts: List<com.cybervictim.assistant.data.CaseFact>, max: Int): Pair<List<com.cybervictim.assistant.data.CaseFact>, Int> {
        if (max <= 0) return emptyList<com.cybervictim.assistant.data.CaseFact>() to facts.size
        return if (facts.size <= max) facts to 0
        else facts.subList(0, max) to (facts.size - max)
    }

    fun generatePdf(ctx: Context, caseId: String, facts: List<com.cybervictim.assistant.data.CaseFact>): File {
        val A4_W = 595; val A4_H = 842
        val doc = PdfDocument()
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; isFakeBoldText = true }
        val head = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; isFakeBoldText = true }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        var pageN = 1
        fun newPage(): Triple<PdfDocument.Page, Canvas, IntArray> {
            val info = PdfDocument.PageInfo.Builder(A4_W, A4_H, pageN).create()
            val pg = doc.startPage(info)
            val y = intArrayOf(50)
            return Triple(pg, pg.canvas, y)
        }
        var first = newPage()
        var pg = first.first
        var cn = first.second
        var yref = first.third
        fun line(text: String, paint: Paint, indent: Int = 0) {
            // naive wrap at ~95 chars
            val chunks = text.chunked(95)
            for (ch in chunks) {
                if (yref[0] > A4_H - 40) {
                    doc.finishPage(pg); pageN++
                    first = newPage(); pg = first.first; cn = first.second; yref = first.third; yref[0] = 50
                }
                cn.drawText(ch, 40f + indent, yref[0].toFloat(), paint)
                yref[0] += if (paint.textSize > 14) 20 else 13
            }
        }
        line("Cybercrime Complaint Pack", title)
        line("Case: $caseId  •  Generated on-device by Cyber Victim Assistant", body)
        yref[0] += 8
        line("INCIDENT FACTS (from the victim's own evidence)", head)
        if (facts.isEmpty()) line("No facts extracted yet.", body)
        val (kept, omitted) = budgetFacts(facts, 2000)
        kept.forEach { f ->
            line("- [${f.type}] ${f.valueText}   (source evidence# ${f.sourceEvidenceId})", body, 10)
        }
        if (omitted > 0) line("…and $omitted more facts stored in the app on this device.", body, 10)
        yref[0] += 10
        line("COMPLAINT TEXT (paste into the portal's incident description)", head)
        val upiF = facts.firstOrNull { it.type == "upi_id" }?.valueText ?: "[UPI ID]"
        val txnF = facts.firstOrNull { it.type == "txn_ref" }?.valueText ?: "[transaction reference]"
        val phoneF = facts.firstOrNull { it.type == "phone" }?.valueText ?: "[fraudster number]"
        val amtF = facts.firstOrNull { it.type == "amount" }?.valueText ?: "[amount]"
        val tsF = facts.firstOrNull { it.type == "timestamp" }?.valueText ?: "[time from evidence]"
        val complaint = "On $tsF, I was defrauded of $amtF. The fraudster used UPI ID $upiF. " +
            "Transaction reference: $txnF. Phone number used by the fraudster: $phoneF. " +
            "Timeline and amounts are attached. Requesting investigation and fund-freeze action under the relevant sections."
        line(complaint, body)
        yref[0] += 10
        line("EVIDENCE INDEX (attached separately / referenced)", head)
        line("Facts above trace to the evidence items stored on the phone. Do not delete originals.", body)
        doc.finishPage(pg)
        val dir = File(ctx.filesDir, "packs").apply { mkdirs() }
        val out = File(dir, "complaint-pack-$caseId.pdf")
        out.outputStream().use { doc.writeTo(it) }
        doc.close()

        // HARD SIZE GUARANTEE: if anything pushed the pack toward the cap
        // (future image embedding, pathological fact text), rebuild with a
        // smaller facts budget until it fits: 2000 -> 400 -> 50 lines.
        // A text-only 50-line page is a few KB, so the cap is always met.
        var budget = 2000
        while (out.length() > TARGET_BYTES && budget > 50) {
            budget = if (budget > 400) 400 else 50
            generatePdf(ctx, caseId, budgetFacts(facts, budget).first)
        }
        require(out.length() <= MAX_BYTES) { "complaint pack exceeded hard cap: " + out.length() }
        return out
    }
}
