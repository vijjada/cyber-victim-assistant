package com.cybervictim.assistant.email

import java.util.Locale
import java.util.regex.Pattern

/**
 * On-device rule-based phishing analyzer.
 * Evaluates RFC 822 email metadata & content against 8 core security rules:
 * 1. SPF / DKIM / DMARC authentication verdicts
 * 2. From vs Reply-To vs Return-Path domain mismatch
 * 3. Lookalike / typosquatted sender domain
 * 4. Display link text vs actual URL mismatch
 * 5. Shortened or raw IP-based destination links
 * 6. Urgent threat / coercive / lure wording
 * 7. Dangerous attachment extensions (.exe, .html, .iso, .zip, etc.)
 * 8. QR code presence
 */
object PhishingAnalyzer {

    private val TARGET_BRANDS = listOf(
        "sbi", "onlinesbi", "hdfc", "icici", "axis", "pnb", "bob", "canara",
        "paytm", "phonepe", "gpay", "google", "microsoft", "amazon", "apple",
        "netflix", "incometax", "uidai", "aadhaar", "indiapost", "whatsapp"
    )

    private val SUSPICIOUS_TLDS = setOf(
        "xyz", "top", "club", "work", "click", "live", "loan", "cam", "bid",
        "guru", "buzz", "cfd", "rest", "support", "online", "site", "vip"
    )

    private val URGENCY_REGEXES = listOf(
        Regex("(?i)\\b(account|card|access|service)\\s+(blocked|suspended|terminated|deactivated|locked)\\b") to "Account blockage / suspension threat",
        Regex("(?i)\\b(kyc|pan|aadhaar)\\s+(update|verification|mandate|expired|pending)\\b") to "Urgent KYC or identity verification demand",
        Regex("(?i)\\b(immediate|urgent|within\\s+\\d+\\s+hours|action\\s+required|final\\s+notice)\\b") to "Coercive countdown / urgent action deadline",
        Regex("(?i)\\b(refund|cashback|prize|lottery|bonus|selected\\s+for|win|won)\\b") to "Financial lure (refund, lottery, or cashback claim)",
        Regex("(?i)\\b(part\\s*time\\s+job|work\\s+from\\s+home|daily\\s+income|telegram\\s+task|salary\\s+\\d+)\\b") to "Fake job / task scam lure",
        Regex("(?i)\\b(unauthorized\\s+transaction|fraud\\s+detected|security\\s+alert)\\b") to "False security alarm"
    )

    fun quickScoreMetadata(metadata: EmailMetadata): Pair<Int, String> {
        val reasons = mutableListOf<String>()
        var score = 0

        val fromEmail = extractEmailAddress(metadata.from)
        val replyToEmail = metadata.replyTo?.let { extractEmailAddress(it) }
        val returnPathEmail = metadata.returnPath?.let { extractEmailAddress(it) }

        val fromDomain = extractDomain(fromEmail)
        val replyToDomain = replyToEmail?.let { extractDomain(it) } ?: fromDomain
        val returnPathDomain = returnPathEmail?.let { extractDomain(it) } ?: fromDomain

        // 1. Auth results
        val authHeader = metadata.authResults ?: ""
        val spfStatus = extractAuthStatus(authHeader, "spf")
        val dkimStatus = extractAuthStatus(authHeader, "dkim")
        val dmarcStatus = extractAuthStatus(authHeader, "dmarc")

        if (spfStatus.contains("fail", ignoreCase = true)) {
            score += 35
            reasons.add("SPF Failed")
        }
        if (dmarcStatus.contains("fail", ignoreCase = true)) {
            score += 35
            reasons.add("DMARC Failed")
        }
        if (dkimStatus.contains("fail", ignoreCase = true)) {
            score += 25
            reasons.add("DKIM Failed")
        }

        // 2. Mismatch
        if (replyToDomain.isNotEmpty() && !isSameEffectiveDomain(fromDomain, replyToDomain)) {
            score += 30
            reasons.add("Reply-To mismatch ($replyToDomain)")
        }
        if (returnPathDomain.isNotEmpty() && !isSameEffectiveDomain(fromDomain, returnPathDomain) && !isKnownMailService(returnPathDomain)) {
            score += 20
            reasons.add("Return-Path mismatch")
        }

        // 3. Lookalike
        val lookalikeInfo = checkLookalikeDomain(fromDomain)
        if (lookalikeInfo != null) {
            score += 35
            reasons.add("Lookalike domain (mimics $lookalikeInfo)")
        } else {
            val tld = fromDomain.substringAfterLast('.', "").lowercase(Locale.ROOT)
            if (tld in SUSPICIOUS_TLDS) {
                score += 15
                reasons.add("Suspicious .$tld domain")
            }
        }

        // 4. Urgency in subject
        for ((regex, desc) in URGENCY_REGEXES) {
            if (regex.containsMatchIn(metadata.subject)) {
                score += 25
                reasons.add(desc)
                break
            }
        }

        val summary = when {
            reasons.isNotEmpty() -> reasons.take(2).joinToString("; ")
            score > 0 -> "Unverified sender origin"
            else -> "Standard authenticated headers"
        }
        return Pair(score, summary)
    }

    fun analyze(email: EmailContent): PhishingAnalysisResult {
        val reasons = mutableListOf<String>()
        val techDetails = mutableListOf<Pair<String, String>>()
        var score = 0

        val fromEmail = extractEmailAddress(email.metadata.from)
        val replyToEmail = email.metadata.replyTo?.let { extractEmailAddress(it) }
        val returnPathEmail = email.metadata.returnPath?.let { extractEmailAddress(it) }

        val fromDomain = extractDomain(fromEmail)
        val replyToDomain = replyToEmail?.let { extractDomain(it) } ?: fromDomain
        val returnPathDomain = returnPathEmail?.let { extractDomain(it) } ?: fromDomain

        techDetails.add("Sender (From)" to email.metadata.from)
        if (email.metadata.replyTo != null) techDetails.add("Reply-To" to email.metadata.replyTo)
        if (email.metadata.returnPath != null) techDetails.add("Return-Path" to email.metadata.returnPath)
        techDetails.add("Subject" to email.metadata.subject)

        // ── RULE 1: SPF, DKIM, DMARC Authentication ──
        val authHeader = email.metadata.authResults ?: ""
        val spfStatus = extractAuthStatus(authHeader, "spf")
        val dkimStatus = extractAuthStatus(authHeader, "dkim")
        val dmarcStatus = extractAuthStatus(authHeader, "dmarc")

        techDetails.add("SPF Verdict" to spfStatus)
        techDetails.add("DKIM Verdict" to dkimStatus)
        techDetails.add("DMARC Verdict" to dmarcStatus)

        var authFailed = false
        if (spfStatus.contains("fail", ignoreCase = true)) {
            score += 35
            authFailed = true
            reasons.add("SPF Authentication Failed: The sending server is NOT authorized by $fromDomain to send email on their behalf.")
        }
        if (dmarcStatus.contains("fail", ignoreCase = true)) {
            score += 35
            authFailed = true
            reasons.add("DMARC Enforcement Failed: Email identity could not be verified against the legitimate domain owner.")
        }
        if (dkimStatus.contains("fail", ignoreCase = true)) {
            score += 25
            authFailed = true
            reasons.add("DKIM Signature Failed: Cryptographic signature was invalid or altered in transit.")
        }

        // ── RULE 2: From vs Reply-To vs Return-Path Mismatch ──
        var domainMismatch = false
        if (replyToDomain.isNotEmpty() && !isSameEffectiveDomain(fromDomain, replyToDomain)) {
            score += 30
            domainMismatch = true
            reasons.add("Reply-To Mismatch: The email claims to be from $fromDomain, but replies will be secretly sent to $replyToDomain.")
            techDetails.add("Reply-To Domain Mismatch" to "$fromDomain vs $replyToDomain")
        }
        if (returnPathDomain.isNotEmpty() && !isSameEffectiveDomain(fromDomain, returnPathDomain) && !isKnownMailService(returnPathDomain)) {
            score += 20
            domainMismatch = true
            reasons.add("Return-Path Divergence: Delivery bounce address points to an unexpected domain: $returnPathDomain.")
            techDetails.add("Return-Path Mismatch" to "$fromDomain vs $returnPathDomain")
        }

        // ── RULE 3: Lookalike or Typosquatted Domain ──
        var lookalikeFound = false
        val lookalikeInfo = checkLookalikeDomain(fromDomain)
        if (lookalikeInfo != null) {
            score += 35
            lookalikeFound = true
            reasons.add("Impersonated / Lookalike Domain: Sender domain '$fromDomain' appears to be impersonating '$lookalikeInfo'.")
            techDetails.add("Lookalike Domain Flag" to "$fromDomain impersonating $lookalikeInfo")
        } else {
            val tld = fromDomain.substringAfterLast('.', "").lowercase(Locale.ROOT)
            if (tld in SUSPICIOUS_TLDS) {
                score += 15
                reasons.add("Suspicious Top-Level Domain: The sender domain uses '.$tld', frequently associated with disposable scam operations.")
                techDetails.add("Suspicious TLD" to ".$tld")
            }
        }

        // ── RULE 4 & 5: Link Mismatches, IP Links, Shorteners ──
        val suspiciousLinks = mutableListOf<EmailLink>()
        for (link in email.extractedLinks) {
            if (link.isMismatch) {
                score += 30
                suspiciousLinks.add(link)
                reasons.add("Deceptive Link: Link showing '${link.displayText}' actually redirects to '${link.targetUrl}'.")
                techDetails.add("Deceptive Link Detected" to "Label: ${link.displayText} -> Target: ${link.targetUrl}")
            }
            if (link.isIpBased) {
                score += 25
                suspiciousLinks.add(link)
                reasons.add("Raw IP Address Link: Contains link pointing to direct IP address (${link.targetUrl}) instead of a verified website.")
                techDetails.add("Raw IP Link" to link.targetUrl)
            }
            if (link.isShortened) {
                score += 15
                suspiciousLinks.add(link)
                reasons.add("Shortened URL: Contains link (${link.targetUrl}) that hides its true destination.")
                techDetails.add("Shortened URL" to link.targetUrl)
            }
        }

        // ── RULE 6: Urgency & Threat Lexicon ──
        val detectedUrgency = mutableListOf<String>()
        val textToScan = "${email.metadata.subject} \n ${email.plainTextBody}"
        for ((regex, desc) in URGENCY_REGEXES) {
            if (regex.containsMatchIn(textToScan)) {
                score += 15
                detectedUrgency.add(desc)
            }
        }
        if (detectedUrgency.isNotEmpty()) {
            reasons.add("Psychological Pressure / Lure: ${detectedUrgency.joinToString(", ")}.")
            techDetails.add("Urgency Triggers" to detectedUrgency.joinToString("; "))
        }

        // ── RULE 7: Dangerous Attachment Types ──
        val riskyAttachments = email.attachments.filter { it.isRiskyType }
        if (riskyAttachments.isNotEmpty()) {
            score += 35
            val names = riskyAttachments.joinToString(", ") { it.fileName }
            reasons.add("High-Risk Attachment: Email contains dangerous executable/script attachment(s): $names.")
            techDetails.add("Dangerous Attachments" to names)
        }

        // ── RULE 8: QR Code Mention / Image ──
        var qrWarning = false
        if (email.hasQrCodeMention) {
            score += 15
            qrWarning = true
            reasons.add("QR Code Notice: Email references or contains a QR code. Fraudsters use QR codes (quishing) to bypass security filters and steal funds.")
            techDetails.add("QR Code Flag" to "Detected in email context")
        }

        // Compute Word-Based Suspicion Level (No traffic light colors!)
        val suspicionLevel = when {
            score >= 50 -> SuspicionLevel.HIGH_SUSPICION
            score >= 25 -> SuspicionLevel.SUSPICIOUS
            score >= 10 -> SuspicionLevel.LOW_SUSPICION
            else -> SuspicionLevel.LEGITIMATE
        }

        val topReasons = if (reasons.isNotEmpty()) reasons.take(3) else listOf("No obvious phishing indicators found. Standard authentication parameters appear consistent.")

        // Actionable guidance for victim
        val nextSteps = buildNextSteps(suspicionLevel, authFailed, domainMismatch, suspiciousLinks.isNotEmpty(), riskyAttachments.isNotEmpty(), qrWarning)

        return PhishingAnalysisResult(
            suspicionLevel = suspicionLevel,
            score = score,
            topReasons = topReasons,
            whatToDoNext = nextSteps,
            technicalDetails = techDetails,
            spfStatus = spfStatus,
            dkimStatus = dkimStatus,
            dmarcStatus = dmarcStatus,
            domainMismatch = domainMismatch,
            lookalikeDomain = lookalikeFound,
            senderDomain = fromDomain,
            replyToDomain = replyToDomain,
            returnPathDomain = returnPathDomain,
            riskyAttachments = riskyAttachments,
            suspiciousLinks = suspiciousLinks,
            urgencyKeywords = detectedUrgency,
            qrCodeDetected = qrWarning
        )
    }

    private fun buildNextSteps(
        level: SuspicionLevel,
        authFailed: Boolean,
        domainMismatch: Boolean,
        hasSuspiciousLinks: Boolean,
        hasRiskyAttachments: Boolean,
        hasQr: Boolean
    ): List<String> {
        val steps = mutableListOf<String>()
        if (level == SuspicionLevel.HIGH_SUSPICION || level == SuspicionLevel.SUSPICIOUS) {
            steps.add("DO NOT click any links or download attachments in this email.")
            if (hasSuspiciousLinks) {
                steps.add("If you clicked any link and entered your password or OTP, immediately change your password and sign out of all active sessions.")
            }
            if (hasRiskyAttachments) {
                steps.add("If you opened or downloaded the attachment, run an antivirus scan on your device immediately.")
            }
            if (hasQr) {
                steps.add("DO NOT scan the QR code with your camera or UPI payment app.")
            }
            steps.add("Use your mail app's official 'Report phishing' or 'Report spam' option.")
            steps.add("Attach the original .eml file to your official National Cyber Crime Reporting Portal complaint.")
        } else {
            steps.add("While no major threats were flagged, always verify unexpected requests through the sender's verified official customer care.")
            steps.add("Never share OTPs, UPI PINs, or net banking passwords over email or phone.")
        }
        return steps
    }

    private fun extractEmailAddress(raw: String): String {
        val matcher = Pattern.compile("<([^>]+)>").matcher(raw)
        return if (matcher.find()) matcher.group(1)?.trim() ?: raw.trim()
        else {
            val tokens = raw.split(" ")
            tokens.firstOrNull { it.contains("@") }?.trim() ?: raw.trim()
        }
    }

    private fun extractDomain(emailAddress: String): String {
        return emailAddress.substringAfter('@', "").trim().lowercase(Locale.ROOT)
    }

    private fun isSameEffectiveDomain(d1: String, d2: String): Boolean {
        if (d1.isEmpty() || d2.isEmpty()) return false
        if (d1 == d2) return true
        val root1 = getBaseDomain(d1)
        val root2 = getBaseDomain(d2)
        return root1 == root2
    }

    private fun getBaseDomain(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) {
            if (parts.size >= 3 && (parts[parts.size - 2] in setOf("co", "com", "org", "net", "gov", "ac", "edu"))) {
                parts.takeLast(3).joinToString(".")
            } else {
                parts.takeLast(2).joinToString(".")
            }
        } else domain
    }

    private fun isKnownMailService(domain: String): Boolean {
        val base = getBaseDomain(domain)
        return base in setOf("google.com", "gmail.com", "outlook.com", "hotmail.com", "yahoo.com", "sendgrid.net", "mailgun.org", "amazonses.com")
    }

    private fun checkLookalikeDomain(domain: String): String? {
        val base = getBaseDomain(domain).substringBeforeLast('.')
        for (brand in TARGET_BRANDS) {
            if (base != brand && (base.contains(brand) || isTyposquat(base, brand))) {
                return brand
            }
        }
        return null
    }

    private fun isTyposquat(actual: String, target: String): Boolean {
        if (actual == target) return false
        val normalizedActual = actual
            .replace('0', 'o')
            .replace('1', 'l')
            .replace("rn", "m")
            .replace("vv", "w")
            .replace("-", "")

        val normalizedTarget = target.replace("-", "")
        if (normalizedActual == normalizedTarget) return true

        // Levenshtein distance check for 1-character typos
        if (levenshteinDistance(actual, target) <= 1 && actual.length >= 4) return true
        return false
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun extractAuthStatus(authHeader: String, protocol: String): String {
        if (authHeader.isBlank()) return "Not Present"
        val regex = Pattern.compile("(?i)\\b$protocol=([a-zA-Z0-9_-]+)")
        val matcher = regex.matcher(authHeader)
        return if (matcher.find()) {
            matcher.group(1)?.uppercase(Locale.ROOT) ?: "Not Present"
        } else {
            "Not Present"
        }
    }
}
