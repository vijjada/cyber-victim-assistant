package com.cybervictim.assistant.email

import android.content.Context
import com.cybervictim.assistant.data.CaseDb
import com.cybervictim.assistant.data.CaseEvent
import com.cybervictim.assistant.data.CaseFact
import com.cybervictim.assistant.data.EvidenceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object EmailStorage {

    /**
     * Saves raw EML message to app-private storage, computes SHA-256 hash,
     * registers evidence item & facts in CaseDb, and logs a timeline event.
     */
    suspend fun saveAndRecord(
        context: Context,
        caseId: String,
        emailContent: EmailContent,
        analysis: PhishingAnalysisResult
    ): EmailEvidenceCard = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "emails").apply { mkdirs() }
        val shaPrefix = emailContent.rawEmlSha256.take(8)
        val fileName = "email_${System.currentTimeMillis()}_$shaPrefix.eml"
        val emlFile = File(dir, fileName)
        emlFile.writeBytes(emailContent.rawEmlBytes)

        val db = CaseDb.get(context).dao()

        // 1. Evidence item
        val subjectClean = emailContent.metadata.subject.take(35).ifBlank { "Untitled Email" }
        val evidenceItem = EvidenceItem(
            caseId = caseId,
            displayName = "E-Mail: $subjectClean",
            uri = emlFile.toURI().toString(),
            kind = "eml",
            extractedFactIds = "[]"
        )
        val evidenceId = db.addEvidence(evidenceItem)

        // 2. Incident facts for complaint pack & portal
        db.addFact(
            CaseFact(
                caseId = caseId,
                type = "email_from",
                valueText = emailContent.metadata.from,
                sourceEvidenceId = evidenceId
            )
        )
        db.addFact(
            CaseFact(
                caseId = caseId,
                type = "email_subject",
                valueText = emailContent.metadata.subject,
                sourceEvidenceId = evidenceId
            )
        )
        db.addFact(
            CaseFact(
                caseId = caseId,
                type = "email_sha256",
                valueText = emailContent.rawEmlSha256,
                sourceEvidenceId = evidenceId
            )
        )
        db.addFact(
            CaseFact(
                caseId = caseId,
                type = "timestamp",
                valueText = emailContent.metadata.dateStr,
                sourceEvidenceId = evidenceId
            )
        )
        db.addFact(
            CaseFact(
                caseId = caseId,
                type = "portal_category",
                valueText = "Online and Social Media Related Crime / E-Mail Phishing",
                sourceEvidenceId = evidenceId
            )
        )

        // Add suspicious links as facts if detected
        analysis.suspiciousLinks.forEach { link ->
            db.addFact(
                CaseFact(
                    caseId = caseId,
                    type = "url",
                    valueText = link.targetUrl,
                    sourceEvidenceId = evidenceId
                )
            )
        }

        // 3. Case timeline event: sits next to other events (e.g. PhoneTrace)
        val eventDetail = "Phishing email received ${emailContent.metadata.dateStr} | '$subjectClean' | Level: ${analysis.suspicionLevel.displayText} | SHA-256: ${emailContent.rawEmlSha256.take(12)}..."
        db.addEvent(
            CaseEvent(
                caseId = caseId,
                kind = "email_phishing",
                detail = eventDetail
            )
        )

        EmailEvidenceCard(
            emailContent = emailContent,
            analysis = analysis,
            savedFilePath = emlFile.absolutePath,
            importTimestamp = System.currentTimeMillis()
        )
    }
}
