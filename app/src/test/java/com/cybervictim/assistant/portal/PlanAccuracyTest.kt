package com.cybervictim.assistant.portal

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 4a test gate: run N realistic victim case texts through the on-device
 * deterministic picker and measure scenario-accuracy. The PDF pitch cites this
 * number ("e.g. 43 out of 50"). Uses kb/scenario_buttons.json as-is.
 */
class PlanAccuracyTest {

    private fun kbJson(): String {
        val candidates = listOf(
            File("../app/src/main/assets/kb/scenario_buttons.json"),
            File("app/src/main/assets/kb/scenario_buttons.json"),
            File("src/main/assets/kb/scenario_buttons.json"),
        )
        return candidates.first { it.exists() }.readText()
    }

    data class Case(val text: String, val expected: String)

    private val cases = listOf(
        Case("I paid 50000 through UPI to a shop QR and he blocked me immediately", "upi_fraud_immediate_transfer"),
        Case("Someone sent me a payment link and took 20000, then his number stopped working", "upi_fraud_immediate_transfer"),
        Case("UPI fraud, money went to the scammer within one hour", "upi_fraud_immediate_transfer"),
        Case("I sold my bike online, buyer paid by UPI, app shows fake payment pending then took OTP", "upi_fraud_immediate_transfer"),
        Case("They promised a refund and I sent 15000 by Google Pay, now no reply", "upi_fraud_immediate_transfer"),
        Case("Got a job offer with a registration fee of 999, then more fees, total 34000 paid by UPI", "fake_job_scam"),
        Case("Telegram part-time job, they took fees again and again via UPI and blocked me", "fake_job_scam"),
        Case("Task-based earning app asks deposits by UPI, cannot withdraw now", "fake_job_scam"),
        Case("Fake HR on WhatsApp charged security deposit for job, paid 8000", "fake_job_scam"),
        Case("Work from home typing job scam, paid registration and then ghosted", "fake_job_scam"),
        Case("Someone threatened me with my private photos and is asking money", "sextortion"),
        Case("Blackmail video call screenshot, threatens to send to family unless I pay", "sextortion"),
        Case("Unknown person has my intimate pictures and blackmails me daily", "sextortion"),
        Case("Instagram friend recorded video chat and threatens to upload", "sextortion"),
        Case("Sextortion gang calling repeatedly demanding 50000", "sextortion"),
        Case("Someone hacked my email ID and I cannot reset the password anymore", "hacked_account_email"),
        Case("My Facebook account is taken over by a hacker and spam messages are sent", "hacked_account_email"),
        Case("Google account hacked, recovery phone and email changed by attacker", "hacked_account_email"),
        Case("WhatsApp OTP came and then my WhatsApp got hijacked", "hacked_account_email"),
        Case("Hacked Instagram profile, attacker changed username and email", "hacked_account_email"),
        Case("Someone opened a loan using my Aadhaar and PAN documents", "identity_misuse"),
        Case("My KYC documents were misused to open a fake bank account", "identity_misuse"),
        Case("Received SIM in someone else's name using my ID proof, identity theft suspected", "identity_misuse"),
        Case("A person is using my PAN card for online verification everywhere", "identity_misuse"),
        Case("Someone cloned my Aadhaar-based eKYC and took a personal loan in my name", "identity_misuse"),
        Case("Online shopping site took payment of 7000 and never delivered the product", "online_shopping_fraud"),
        Case("Ordered shoes from an Instagram page, paid by UPI, seller vanished", "online_shopping_fraud"),
        Case("Fake customer care number found on Google, refunded 12000 to them instead", "online_shopping_fraud"),
        Case("Bought a phone from a random website, payment done, no product, no contact", "online_shopping_fraud"),
        Case("OLX buyer sent fake payment screenshot and took my stuff", "online_shopping_fraud"),
        Case("Instant loan app calling my contacts and abusing me for 5 days", "loanapp_harassment"),
        Case("Loan app shows 25000 disbursed but I received 14000, now harassment calls", "loanapp_harassment"),
        Case("Chinese loan app contacts accessed my phone and threatened defamation", "loanapp_harassment"),
        Case("Took small loan in app, repaid, still getting recovery threats for others' loans", "loanapp_harassment"),
        Case("Loan app blackmailed me with my contacts' photos, threatening calls continue", "loanapp_harassment"),
        Case("My edited photos without clothes are circulating in a public Telegram channel", "photo_leak_no_money"),
        Case("Someone made a fake nude picture of me using AI and shared it", "photo_leak_no_money"),
        Case("Deepfake video of me is spreading on WhatsApp groups", "photo_leak_no_money"),
        Case("Private pictures leaked by my ex boyfriend on social media, no money involved", "photo_leak_no_money"),
        Case("Anonymous person uploaded my photos on an obscene website", "photo_leak_no_money"),
    )

    @Test
    fun accuracyAtLeast90Percent() {
        val kb = kbJson()
        var hits = 0
        val misses = mutableListOf<String>()
        for (c in cases) {
            val plan = PlanBuilder.fromKbJson(kb, c.text)
            if (plan.scenarioId == c.expected) hits++ else misses.add("${c.expected} got ${plan.scenarioId} :: ${c.text}")
        }
        println("SCENARIO-ACCURACY ${hits}/${cases.size} = ${"%.1f".format(hits * 100.0 / cases.size)}%")
        if (misses.isNotEmpty()) println(misses.joinToString("\n"))
        assertTrue("expected >= 90%, got ${hits}/${cases.size}", hits * 1.0 / cases.size >= 0.90)
    }
}
