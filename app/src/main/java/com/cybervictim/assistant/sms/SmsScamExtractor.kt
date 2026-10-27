package com.cybervictim.assistant.sms

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.core.content.ContextCompat
import com.cybervictim.assistant.ai.CaseSessionStore
import com.cybervictim.assistant.data.CaseDb
import com.cybervictim.assistant.data.CaseFact
import com.cybervictim.assistant.data.CaseSession
import com.cybervictim.assistant.data.EvidenceItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class ExtractedScamData(
    val amount: String,
    val amountNum: Double,
    val bankName: String,
    val dateTimeFormatted: String,
    val timestampMillis: Long,
    val utrRef: String,
    val fraudsterId: String,
    val smsSender: String,
    val smsBody: String,
    val scamType: String,
)

object SmsScamExtractor {

    private val BANK_SENDER_PATTERNS = listOf(
        "SBI", "HDFC", "ICICI", "AXIS", "KOTAK", "PAYTM", "PHONEPE", "GPAY",
        "PNB", "BOB", "CANARA", "UNION", "INDUS", "YESB", "IDFC", "FEDBNK"
    )

    private val DEBIT_KEYWORDS = listOf(
        "debited", "transferred", "paid", "sent", "withdrawn", "spent",
        "deducted", "upi", "txn", "ref", "utr", "vpa"
    )

    /**
     * Parse natural language date/time string like "Yesterday around six in the evening",
     * "Today at 11 AM", etc. into an approximate epoch timestamp in milliseconds.
     */
    fun parseTargetDateTime(text: String): Long {
        val lower = text.lowercase(Locale.ENGLISH).trim()
        val cal = Calendar.getInstance()

        if (lower.contains("yesterday")) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else if (lower.contains("2 days ago") || lower.contains("two days ago")) {
            cal.add(Calendar.DAY_OF_YEAR, -2)
        }

        // Time of day heuristic
        var hour = 12
        var minute = 0

        val timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val matcher = timePattern.matcher(lower)
        if (matcher.find()) {
            val h = matcher.group(1)?.toIntOrNull() ?: 12
            val m = matcher.group(2)?.toIntOrNull() ?: 0
            val ampm = matcher.group(3)?.lowercase()
            hour = when {
                ampm == "pm" && h < 12 -> h + 12
                ampm == "am" && h == 12 -> 0
                else -> h
            }
            minute = m
        } else {
            // Check words like "six in the evening", "morning", "afternoon", "night"
            val wordHour = when {
                lower.contains("one") -> 1
                lower.contains("two") -> 2
                lower.contains("three") -> 3
                lower.contains("four") -> 4
                lower.contains("five") -> 5
                lower.contains("six") -> 6
                lower.contains("seven") -> 7
                lower.contains("eight") -> 8
                lower.contains("nine") -> 9
                lower.contains("ten") -> 10
                lower.contains("eleven") -> 11
                lower.contains("twelve") -> 12
                else -> 12
            }
            hour = when {
                lower.contains("evening") || lower.contains("night") -> if (wordHour < 12) wordHour + 12 else wordHour
                lower.contains("afternoon") -> if (wordHour < 12) wordHour + 12 else wordHour
                lower.contains("morning") -> if (wordHour == 12) 0 else wordHour
                else -> wordHour
            }
        }

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Scans the Android SMS inbox for transactions matching the mentioned date/time
     * and extracts amount, bank, UTR, fraudster UPI/account, and exact timestamp.
     */
    fun scanInboxForScamData(context: Context, dateMention: String): List<ExtractedScamData> {
        val results = mutableListOf<ExtractedScamData>()
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val targetTime = parseTargetDateTime(dateMention)
        // Window: ±24 hours around target time
        val window = 24L * 60 * 60 * 1000

        if (hasPerm) {
            try {
                val uri = Uri.parse("content://sms/inbox")
                val projection = arrayOf("_id", "address", "date", "body")
                val cursor: Cursor? = context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "date DESC"
                )

                cursor?.use { c ->
                    val addressIdx = c.getColumnIndex("address")
                    val dateIdx = c.getColumnIndex("date")
                    val bodyIdx = c.getColumnIndex("body")

                    var count = 0
                    while (c.moveToNext() && count < 200) {
                        count++
                        val address = if (addressIdx >= 0) c.getString(addressIdx) ?: "" else ""
                        val dateMillis = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                        val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""

                        val bodyLower = body.lowercase()
                        val isDebitOrScam = DEBIT_KEYWORDS.any { bodyLower.contains(it) } ||
                                BANK_SENDER_PATTERNS.any { address.uppercase().contains(it) }

                        if (isDebitOrScam) {
                            val parsed = extractFromSmsBody(address, dateMillis, body)
                            if (parsed != null) {
                                // Score by proximity to target time
                                val timeDiff = Math.abs(dateMillis - targetTime)
                                if (timeDiff <= window * 2) {
                                    results.add(parsed)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return results
    }

    /**
     * Extracts structured fields from an SMS text.
     */
    fun extractFromSmsBody(address: String, timestamp: Long, body: String): ExtractedScamData? {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val formattedDate = sdf.format(Date(timestamp))

        // 1. Amount Extraction (e.g. Rs. 25,000 or INR 500.00 or Rs 10000)
        var amountStr = ""
        var amountNum = 0.0
        val amountPattern = Pattern.compile("(?:Rs\\.?|INR|\\₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
        val amtMatch = amountPattern.matcher(body)
        if (amtMatch.find()) {
            val raw = amtMatch.group(1)?.replace(",", "") ?: ""
            amountNum = raw.toDoubleOrNull() ?: 0.0
            amountStr = "Rs. " + (amtMatch.group(1) ?: "")
        }

        // 2. Bank Name Extraction
        var bank = "Bank / UPI"
        val upperAddr = address.uppercase()
        when {
            upperAddr.contains("SBI") || body.contains("State Bank", ignoreCase = true) -> bank = "State Bank of India"
            upperAddr.contains("HDFC") || body.contains("HDFC", ignoreCase = true) -> bank = "HDFC Bank"
            upperAddr.contains("ICICI") || body.contains("ICICI", ignoreCase = true) -> bank = "ICICI Bank"
            upperAddr.contains("AXIS") || body.contains("Axis", ignoreCase = true) -> bank = "Axis Bank"
            upperAddr.contains("KOTAK") || body.contains("Kotak", ignoreCase = true) -> bank = "Kotak Mahindra Bank"
            upperAddr.contains("PNB") || body.contains("Punjab National", ignoreCase = true) -> bank = "Punjab National Bank"
            upperAddr.contains("BOB") || body.contains("Bank of Baroda", ignoreCase = true) -> bank = "Bank of Baroda"
            upperAddr.contains("PAYTM") || body.contains("Paytm", ignoreCase = true) -> bank = "Paytm Payments Bank"
            upperAddr.contains("PHONEPE") || body.contains("PhonePe", ignoreCase = true) -> bank = "PhonePe"
            else -> {
                if (address.isNotBlank()) bank = address
            }
        }

        // 3. UTR / Txn Reference Extraction (12-digit UPI reference or txn id)
        var utr = ""
        val utrPattern = Pattern.compile("(?:UPI Ref(?: no)?|Ref(?: no)?|UTR|Txn(?: ID)?|Reference)[:\\s]*([0-9]{9,16})", Pattern.CASE_INSENSITIVE)
        val utrMatch = utrPattern.matcher(body)
        if (utrMatch.find()) {
            utr = utrMatch.group(1) ?: ""
        } else {
            // Find any 12-digit consecutive number
            val num12 = Pattern.compile("\\b(\\d{12})\\b")
            val numMatch = num12.matcher(body)
            if (numMatch.find()) {
                utr = numMatch.group(1) ?: ""
            }
        }

        // 4. Fraudster UPI or account (e.g. to xyz@upi or VPA xyz@okhdfcbank)
        var fraudster = ""
        val upiPattern = Pattern.compile("(?:to|VPA|transfer to|beneficiary)\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE)
        val upiMatch = upiPattern.matcher(body)
        if (upiMatch.find()) {
            fraudster = upiMatch.group(1) ?: ""
        }

        if (amountStr.isBlank() && utr.isBlank()) {
            return null
        }

        return ExtractedScamData(
            amount = if (amountStr.isNotBlank()) amountStr else "Unknown Amount",
            amountNum = amountNum,
            bankName = bank,
            dateTimeFormatted = formattedDate,
            timestampMillis = timestamp,
            utrRef = if (utr.isNotBlank()) utr else "Ref in SMS",
            fraudsterId = if (fraudster.isNotBlank()) fraudster else "Account in SMS",
            smsSender = address,
            smsBody = body,
            scamType = if (body.contains("upi", ignoreCase = true)) "UPI Debit Fraud" else "Bank Account Debit"
        )
    }

    /**
     * Persist the extracted scam data into Room DB CaseDb and CaseSessionStore.
     */
    fun applyToCase(context: Context, data: ExtractedScamData) {
        val caseId = CaseSession.ensure()
        val db = CaseDb.get(context)

        CoroutineScope(Dispatchers.IO).launch {
            // 1. Add Facts
            if (data.amount.isNotBlank()) {
                db.dao().addFact(CaseFact(caseId = caseId, type = "amount", valueText = data.amount, sourceEvidenceId = 0, confidence = 1f))
            }
            if (data.bankName.isNotBlank()) {
                db.dao().addFact(CaseFact(caseId = caseId, type = "other", valueText = "Bank: ${data.bankName}", sourceEvidenceId = 0, confidence = 1f))
            }
            if (data.dateTimeFormatted.isNotBlank()) {
                db.dao().addFact(CaseFact(caseId = caseId, type = "timestamp", valueText = data.dateTimeFormatted, sourceEvidenceId = 0, confidence = 1f))
            }
            if (data.utrRef.isNotBlank()) {
                db.dao().addFact(CaseFact(caseId = caseId, type = "txn_ref", valueText = data.utrRef, sourceEvidenceId = 0, confidence = 1f))
            }
            if (data.fraudsterId.isNotBlank()) {
                db.dao().addFact(CaseFact(caseId = caseId, type = "upi_id", valueText = data.fraudsterId, sourceEvidenceId = 0, confidence = 1f))
            }

            // 2. Add as Evidence item
            db.dao().addEvidence(
                EvidenceItem(
                    caseId = caseId,
                    displayName = "Bank SMS Evidence (${data.bankName})",
                    uri = "sms://${data.smsSender}/${data.timestampMillis}",
                    kind = "sms",
                    extractedFactIds = "[]"
                )
            )

            // 3. Feed to AI CaseSessionStore
            CaseSessionStore.current?.onUserMessage(
                "Extracted from bank SMS (${data.smsSender}): Amount=${data.amount}, Bank=${data.bankName}, Date=${data.dateTimeFormatted}, UTR=${data.utrRef}, Beneficiary=${data.fraudsterId}. SMS Text: ${data.smsBody}"
            )
        }
    }
}
