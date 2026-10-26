package com.cybervictim.assistant.ai

import com.cybervictim.assistant.data.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ComplaintScriptGeneratorTest {

    @Test
    fun testFinancialFraudLegalNarrativeContainsStatutoryProvisions() {
        val profile = UserProfile(
            fullName = "Rajesh Sharma",
            mobile = "9876543210",
            email = "rajesh@example.com",
            state = "Maharashtra",
            district = "Mumbai"
        )

        val narrative = ComplaintScriptGenerator.buildLegalNarrative(
            profile = profile,
            amount = "Rs. 45,000",
            bank = "HDFC Bank",
            dateTime = "24/10/2026 14:30",
            fraudster = "fraudster@ybl",
            utr = "987654321098",
            contactMethod = "WhatsApp task scam group",
            compromiseMethod = "Induced UPI transfer under promise of high returns"
        )

        assertTrue("Must include complainant name", narrative.contains("Rajesh Sharma"))
        assertTrue("Must include mobile number", narrative.contains("9876543210"))
        assertTrue("Must include defrauded amount", narrative.contains("Rs. 45,000"))
        assertTrue("Must include debited bank", narrative.contains("HDFC Bank"))
        assertTrue("Must include fraudster UPI", narrative.contains("fraudster@ybl"))
        assertTrue("Must include UTR number", narrative.contains("987654321098"))
        assertTrue("Must cite Section 1930 CFCFRMS protocol", narrative.contains("1930"))
        assertTrue("Must cite Section 66D IT Act", narrative.contains("66D"))
        assertTrue("Must cite Section 318(4) BNS", narrative.contains("318(4)"))
        assertTrue("Must request account lien-mark/freeze", narrative.contains("freeze"))
    }

    @Test
    fun testHackedAccountNarrativeContainsSection66C() {
        val profile = UserProfile(
            fullName = "Pooja Patel",
            mobile = "9123456780",
            state = "Gujarat",
            district = "Ahmedabad"
        )

        val narrative = ComplaintScriptGenerator.buildLegalNarrative(
            profile = profile,
            amount = "N/A",
            bank = "None",
            dateTime = "Yesterday 8 PM",
            fraudster = "@pooja_hijacked",
            utr = "N/A",
            contactMethod = "Instagram DM phishing link",
            compromiseMethod = "Account hacked and password changed"
        )

        assertTrue("Must cite Section 66C IT Act", narrative.contains("66C"))
        assertTrue("Must include complainant name", narrative.contains("Pooja Patel"))
        assertTrue("Must cite Section 91 CrPC/BNSS preservation", narrative.contains("Section 91"))
    }

    @Test
    fun testGenerateScriptCompletesWithoutHanging() = runBlocking {
        val profile = UserProfile(
            fullName = "Anil Verma",
            mobile = "9988776655",
            state = "Delhi",
            district = "New Delhi"
        )

        val script = ComplaintScriptGenerator.generateScript(
            profile = profile,
            amount = "Rs. 15,000",
            bank = "State Bank of India",
            dateTime = "Today 10 AM",
            fraudster = "scam@paytm",
            utr = "112233445566",
            contactMethod = "Fake electricity bill SMS",
            compromiseMethod = "Clicked malicious link"
        )

        assertNotNull(script)
        assertTrue("Script length should be substantial", script.length > 100)
        assertTrue("Script should contain 1930", script.contains("1930"))
    }
}
