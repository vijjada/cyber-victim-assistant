package com.cybervictim.assistant.email

/**
 * Data structures for Suspicious Email Check.
 * Built with strict privacy:
 * - Word-based suspicion ratings (no traffic light colors)
 * - Raw EML with SHA-256 for evidence pack
 * - No active link pre-fetching or attachment execution
 */

enum class SuspicionLevel(val displayText: String) {
    HIGH_SUSPICION("High Suspicion"),
    SUSPICIOUS("Suspicious"),
    LOW_SUSPICION("Low Suspicion"),
    LEGITIMATE("Legitimate / Normal")
}

data class EmailMetadata(
    val id: String,
    val from: String,
    val replyTo: String? = null,
    val returnPath: String? = null,
    val to: String? = null,
    val subject: String,
    val dateStr: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val messageIdHeader: String? = null,
    val authResults: String? = null,
    val receivedHeaders: List<String> = emptyList(),
    val quickSuspicionReason: String? = null,
    val quickScore: Int = 0
)

data class AttachmentMetadata(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val isRiskyType: Boolean,
    val sha256: String? = null
)

data class EmailLink(
    val displayText: String,
    val targetUrl: String,
    val isMismatch: Boolean = false,
    val isIpBased: Boolean = false,
    val isShortened: Boolean = false
)

data class EmailContent(
    val metadata: EmailMetadata,
    val plainTextBody: String,
    val extractedLinks: List<EmailLink> = emptyList(),
    val attachments: List<AttachmentMetadata> = emptyList(),
    val rawEmlBytes: ByteArray,
    val rawEmlSha256: String,
    val hasQrCodeMention: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EmailContent
        return metadata.id == other.metadata.id && rawEmlSha256 == other.rawEmlSha256
    }

    override fun hashCode(): Int {
        var result = metadata.id.hashCode()
        result = 31 * result + rawEmlSha256.hashCode()
        return result
    }
}

data class PhishingAnalysisResult(
    val suspicionLevel: SuspicionLevel,
    val score: Int,
    val topReasons: List<String>,
    val whatToDoNext: List<String>,
    val technicalDetails: List<Pair<String, String>>,
    val spfStatus: String = "Not Present",
    val dkimStatus: String = "Not Present",
    val dmarcStatus: String = "Not Present",
    val domainMismatch: Boolean = false,
    val lookalikeDomain: Boolean = false,
    val senderDomain: String = "",
    val replyToDomain: String = "",
    val returnPathDomain: String = "",
    val riskyAttachments: List<AttachmentMetadata> = emptyList(),
    val suspiciousLinks: List<EmailLink> = emptyList(),
    val urgencyKeywords: List<String> = emptyList(),
    val qrCodeDetected: Boolean = false
)

data class EmailEvidenceCard(
    val emailContent: EmailContent,
    val analysis: PhishingAnalysisResult,
    val savedFilePath: String,
    val importTimestamp: Long = System.currentTimeMillis()
)
