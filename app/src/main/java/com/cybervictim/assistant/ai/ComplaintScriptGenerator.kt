package com.cybervictim.assistant.ai

import com.cybervictim.assistant.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object ComplaintScriptGenerator {

    /**
     * Generates a structured, legally sound cyber crime complaint script ready
     * for filing on https://cybercrime.gov.in/dashboard/crimereport.
     * Always generates an instant, robust, police-grade legal narrative without hanging.
     */
    suspend fun generateScript(
        profile: UserProfile,
        amount: String,
        bank: String,
        dateTime: String,
        fraudster: String,
        utr: String,
        contactMethod: String,
        compromiseMethod: String,
        additionalNotes: String = ""
    ): String = withContext(Dispatchers.IO) {
        // Generate high-grade on-device narrative immediately
        val offlineNarrative = buildLegalNarrative(
            profile = profile,
            amount = amount,
            bank = bank,
            dateTime = dateTime,
            fraudster = fraudster,
            utr = utr,
            contactMethod = contactMethod,
            compromiseMethod = compromiseMethod,
            additionalNotes = additionalNotes
        )

        // Try AI enhancement with strict 2.5 second timeout; never block or hang
        val aiResult = try {
            withTimeoutOrNull(2500) {
                val prompt = """
                    Refine this police complaint narrative for India's National Cyber Crime Reporting Portal (cybercrime.gov.in).
                    Keep all exact facts (Names, Mobile, Amount $amount, Bank $bank, Date $dateTime, UTR $utr, Fraudster $fraudster).
                    Ensure it requests emergency freeze under Section 1930 / BNS 318(4) / IT Act 66D.
                    Under 200 words. Output ONLY the complaint text without markdown headers.

                    Base text:
                    $offlineNarrative
                """.trimIndent()
                GeminiClient.generate(prompt, 500)?.trim()
            }
        } catch (_: Exception) {
            null
        }

        if (!aiResult.isNullOrBlank() && aiResult.length > 80) {
            aiResult
        } else {
            offlineNarrative
        }
    }

    /**
     * Police-grade legal narrative formatted to exact cybercrime.gov.in standards.
     * Instantaneous, zero network dependency, legally sound.
     */
    fun buildLegalNarrative(
        profile: UserProfile,
        amount: String,
        bank: String,
        dateTime: String,
        fraudster: String,
        utr: String,
        contactMethod: String,
        compromiseMethod: String,
        additionalNotes: String = ""
    ): String {
        val victimName = profile.fullName.ifBlank { "Complainant" }
        val victimPhone = if (profile.mobile.isNotBlank()) profile.mobile else "Registered Mobile"
        val location = buildString {
            if (profile.district.isNotBlank()) append(profile.district)
            if (profile.state.isNotBlank()) {
                if (isNotEmpty()) append(", ")
                append(profile.state)
            }
        }.ifBlank { "India" }

        val amt = amount.ifBlank { "[Amount as per bank statement]" }
        val bnk = bank.ifBlank { "Bank / UPI Account" }
        val dt = dateTime.ifBlank { "the date of incident" }
        val frd = fraudster.ifBlank { "suspect account / UPI ID" }
        val ref = utr.ifBlank { "[Ref/UTR as per bank statement]" }
        val contact = contactMethod.ifBlank { "online communication" }
        val method = compromiseMethod.ifBlank { "deceptive inducement" }

        val isHacked = method.contains("hack", ignoreCase = true) ||
                contact.contains("Instagram", ignoreCase = true) ||
                contact.contains("account", ignoreCase = true) ||
                amt.equals("0", ignoreCase = true) || amt.contains("N/A", ignoreCase = true)

        return if (isHacked) {
            "COMPLAINT UNDER SECTION 66C / 66D INFORMATION TECHNOLOGY ACT:\n" +
            "I, $victimName (Mobile: $victimPhone, Resident of $location), report unauthorized access and hijacking of my account. " +
            "On $dt, I was contacted through $contact and compromised via $method. The suspect obtained unauthorized control, " +
            "altered recovery credentials, and locked me out. Target handle / identifier: $frd. " +
            (if (additionalNotes.isNotBlank()) "Additional details: $additionalNotes. " else "") +
            "I urgently request cyber crime authorities to issue directives for IP/session preservation under Section 91 CrPC/BNSS and order immediate restoration of access."
        } else {
            "COMPLAINT UNDER SECTION 1930 CFCFRMS / SEC 66D IT ACT / SEC 318(4) BNS:\n" +
            "I, $victimName (Mobile: $victimPhone, Resident of $location), wish to report an incident of financial cyber fraud. " +
            "On $dt, I was targeted through $contact and induced via $method. An unauthorized financial loss of $amt was debited from my $bnk account " +
            "and transferred to suspect UPI / Beneficiary: $frd with Transaction UTR reference: $ref. " +
            "Following the transfer, the suspect became unresponsive and blocked all contact. " +
            (if (additionalNotes.isNotBlank()) "Notes: $additionalNotes. " else "") +
            "PRAYER: I request cyber police and banking nodal officers to immediately lien-mark and freeze the beneficiary account under the 1930 CFCFRMS protocol and initiate recovery of the defrauded funds."
        }
    }

    fun generateOfflineFallback(
        profile: UserProfile,
        amount: String,
        bank: String,
        dateTime: String,
        fraudster: String,
        utr: String,
        contactMethod: String,
        compromiseMethod: String
    ): String = buildLegalNarrative(
        profile = profile,
        amount = amount,
        bank = bank,
        dateTime = dateTime,
        fraudster = fraudster,
        utr = utr,
        contactMethod = contactMethod,
        compromiseMethod = compromiseMethod
    )
}
