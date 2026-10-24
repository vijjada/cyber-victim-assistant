package com.cybervictim.assistant.requests

import com.cybervictim.assistant.data.CaseFact

/**
 * Phase 5: ready-to-send drafts built only from the victim's own case facts.
 * Fully local templating — nothing leaves the phone.
 */
object RequestDrafter {

    const val FREEZE = "freeze"
    const val UNFREEZE = "unfreeze"
    const val FOLLOWUP = "followup"
    const val DOCS = "docs"

    fun draft(kind: String, caseId: String, facts: List<CaseFact>): String {
        val upi = facts.firstOrNull { it.type == "upi_id" }?.valueText ?: "<UPI ID>"
        val phone = facts.firstOrNull { it.type == "phone" }?.valueText ?: "<fraudster number>"
        val txn = facts.firstOrNull { it.type == "txn_ref" }?.valueText ?: "<transaction reference>"
        val amt = facts.firstOrNull { it.type == "amount" }?.valueText ?: "<amount>"
        val ts = facts.firstOrNull { it.type == "timestamp" }?.valueText ?: "<time of transaction>"

        return when (kind) {
            FREEZE -> """
                Subject: Urgent fund freeze request — fraud transaction $txn

                Respected Sir/Madam,
                I am a victim of cyber financial fraud (Case: $caseId).
                $amt was paid via UPI to $upi at $ts, in a fraud transaction by/from $phone.
                Request you to FREEZE the recipient account and initiate the chargeback
                process as per RBI guidelines. The amount may still be frozen if acted
                within the golden hour.
                Complaint filed/scheduled at cybercrime.gov.in. Kindly acknowledge.
                Sincerely,
                [Victim name] — [resolved from portal registration]
            """.trimIndent()

            UNFREEZE -> """
                Subject: Request to unfreeze my account (case-linked freeze)

                Respected Sir/Madam,
                My account was frozen in connection with cybercrime case $caseId.
                I am the complainant/victim in this matter and request unfreezing of my
                legitimate funds, or an official status update with the relevant case
                reference. I can provide KYC and the complaint acknowledgment.
                Sincerely,
                [Victim name]
            """.trimIndent()

            FOLLOWUP -> """
                Subject: Follow-up on complaint $caseId

                Respected Sir/Madam,
                Kindly share the current status of complaint $caseId filed on
                cybercrime.gov.in, along with the assigned investigating officer
                details and any documents pending from my side.
                Sincerely,
                [Victim name]
            """.trimIndent()

            else -> """
                Documents commonly asked by the cyber cell / bank — keep ready:
                 1. Complaint acknowledgment number (from cybercrime.gov.in)
                 2. Screenshots of chats/SMS with the fraudster ($phone)
                 3. Bank statement highlighting the fraudulent transaction ($txn, $amt)
                 4. UPI payment screenshot showing $upi
                 5. Government ID (Aadhaar/PAN) copy
                 6. Mobile number registered with the bank
                 7. FIR copy if filed physically
            """.trimIndent()
        }
    }
}
