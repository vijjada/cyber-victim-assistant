package com.cybervictim.assistant.email

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

class GmailApiClientTest {

    @Before
    fun setUp() {
        GmailApiClient.disconnect()
    }

    @Test
    fun testWindowValidation() {
        val now = 1700000000L
        val oneDayLater = now + 86400L
        val eightDaysLater = now + (8 * 86400L)

        // Valid window within 7 days
        val validRes = GmailApiClient.validateWindow(now, oneDayLater)
        assertTrue(validRes.isSuccess)

        // Invalid: start after end
        val invertedRes = GmailApiClient.validateWindow(oneDayLater, now)
        assertTrue(invertedRes.isFailure)
        assertTrue(invertedRes.exceptionOrNull()!!.message!!.contains("Start time cannot be after end time"))

        // Invalid: > 7 days hard cap
        val exceededRes = GmailApiClient.validateWindow(now, eightDaysLater)
        assertTrue(exceededRes.isFailure)
        assertTrue(exceededRes.exceptionOrNull()!!.message!!.contains("Search window cannot exceed 7 days"))
    }

    @Test
    fun testPkceGeneration() {
        val verifier = GmailOAuthHelper.generateCodeVerifier()
        assertNotNull(verifier)
        assertTrue(verifier.length >= 43)

        val challenge = GmailOAuthHelper.generateCodeChallenge(verifier)
        assertNotNull(challenge)
        assertTrue(challenge.isNotBlank())

        val state = GmailOAuthHelper.generateState()
        assertTrue(state.isNotBlank())

        val authUrl = GmailOAuthHelper.buildAuthUrl(
            clientId = "test-client-id.apps.googleusercontent.com",
            codeChallenge = challenge,
            state = state
        )

        assertTrue(authUrl.contains("accounts.google.com"))
        assertTrue(authUrl.contains("test-client-id"))
        assertTrue(authUrl.contains("scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fgmail.readonly"))
        assertTrue(authUrl.contains("code_challenge_method=S256"))
    }

    @Test
    fun testMockTokenExchangeAndRevocation() {
        val mockTransport = object : GmailTransport {
            override fun executeGet(url: String, headers: Map<String, String>): Pair<Int, String> {
                return Pair(200, "{}")
            }

            override fun executePost(url: String, headers: Map<String, String>, body: String): Pair<Int, String> {
                if (url.contains("token")) {
                    val resp = JSONObject().apply {
                        put("access_token", "ya29.mock_access_token_12345")
                        put("token_type", "Bearer")
                        put("expires_in", 3600)
                    }
                    return Pair(200, resp.toString())
                }
                if (url.contains("revoke")) {
                    return Pair(200, "")
                }
                return Pair(404, "")
            }
        }

        val tokenRes = GmailOAuthHelper.exchangeCodeForToken(
            clientId = "client-123",
            redirectUri = "com.cybervictim.assistant:/oauth2redirect",
            code = "auth_code_xyz",
            codeVerifier = "verifier_xyz",
            transport = mockTransport
        )

        assertTrue(tokenRes.isSuccess)
        val token = tokenRes.getOrNull()!!
        assertEquals("ya29.mock_access_token_12345", token)

        val revoked = GmailOAuthHelper.revokeToken(token, mockTransport)
        assertTrue(revoked)
    }

    @Test
    fun testListMetadataOnlyAndSort() = runBlocking {
        GmailApiClient.setToken("mock_token")

        val mockTransport = object : GmailTransport {
            override fun executeGet(url: String, headers: Map<String, String>): Pair<Int, String> {
                // Assert auth header exists
                assertEquals("Bearer mock_token", headers["Authorization"])

                if (url.contains("/messages?q=")) {
                    // Assert query params includeSpamTrash=true
                    assertTrue(url.contains("includeSpamTrash=true"))
                    val resp = JSONObject().apply {
                        val arr = JSONArray()
                        arr.put(JSONObject().put("id", "msg_phish_1"))
                        arr.put(JSONObject().put("id", "msg_legit_2"))
                        put("messages", arr)
                    }
                    return Pair(200, resp.toString())
                }

                if (url.contains("/messages/msg_phish_1?format=metadata")) {
                    val resp = JSONObject().apply {
                        put("id", "msg_phish_1")
                        put("internalDate", 1700001000L)
                        val payload = JSONObject().apply {
                            val headersArr = JSONArray().apply {
                                put(JSONObject().put("name", "From").put("value", "support@sbi-kyc-update.top"))
                                put(JSONObject().put("name", "Subject").put("value", "URGENT: Your Account Has Been Blocked"))
                                put(JSONObject().put("name", "Authentication-Results").put("value", "spf=fail; dmarc=fail"))
                                put(JSONObject().put("name", "Date").put("value", "Wed, 15 Nov 2026 10:00:00 +0530"))
                            }
                            put("headers", headersArr)
                        }
                        put("payload", payload)
                    }
                    return Pair(200, resp.toString())
                }

                if (url.contains("/messages/msg_legit_2?format=metadata")) {
                    val resp = JSONObject().apply {
                        put("id", "msg_legit_2")
                        put("internalDate", 1700000000L)
                        val payload = JSONObject().apply {
                            val headersArr = JSONArray().apply {
                                put(JSONObject().put("name", "From").put("value", "news@example.com"))
                                put(JSONObject().put("name", "Subject").put("value", "Weekly Security Digest"))
                                put(JSONObject().put("name", "Authentication-Results").put("value", "spf=pass; dmarc=pass"))
                                put(JSONObject().put("name", "Date").put("value", "Wed, 15 Nov 2026 09:00:00 +0530"))
                            }
                            put("headers", headersArr)
                        }
                        put("payload", payload)
                    }
                    return Pair(200, resp.toString())
                }

                return Pair(404, "Not found")
            }

            override fun executePost(url: String, headers: Map<String, String>, body: String): Pair<Int, String> {
                return Pair(200, "")
            }
        }

        val candidatesRes = GmailApiClient.listMetadataOnly(
            startEpochSec = 1700000000L,
            endEpochSec = 1700086400L,
            transport = mockTransport
        )

        assertTrue(candidatesRes.isSuccess)
        val candidates = candidatesRes.getOrNull()!!
        assertEquals(2, candidates.size)

        // Phishing candidate should be sorted first due to higher suspicion score
        assertEquals("msg_phish_1", candidates[0].id)
        assertTrue(candidates[0].metadata.quickScore > candidates[1].metadata.quickScore)
        assertTrue(candidates[0].metadata.quickSuspicionReason!!.contains("SPF Failed"))
    }

    @Test
    fun testFetchRawMessageAndDiscardUnticked() = runBlocking {
        GmailApiClient.setToken("mock_token")

        val rawEmlText = "From: attacker@evil.com\r\nSubject: Test\r\n\r\nHello"
        val base64UrlRaw = Base64.getUrlEncoder().withoutPadding().encodeToString(rawEmlText.toByteArray(Charsets.UTF_8))

        val mockTransport = object : GmailTransport {
            override fun executeGet(url: String, headers: Map<String, String>): Pair<Int, String> {
                if (url.contains("/messages/msg_ticked_1?format=raw")) {
                    val resp = JSONObject().apply {
                        put("id", "msg_ticked_1")
                        put("raw", base64UrlRaw)
                    }
                    return Pair(200, resp.toString())
                }
                return Pair(404, "Not found")
            }

            override fun executePost(url: String, headers: Map<String, String>, body: String): Pair<Int, String> {
                return Pair(200, "")
            }
        }

        val rawRes = GmailApiClient.fetchRawMessage("msg_ticked_1", mockTransport)
        assertTrue(rawRes.isSuccess)
        assertEquals(rawEmlText, rawRes.getOrNull())

        // Test discarding unticked candidates
        val meta1 = EmailMetadata("id_1", from = "a@a.com", subject = "Sub 1", dateStr = "today")
        val meta2 = EmailMetadata("id_2", from = "b@b.com", subject = "Sub 2", dateStr = "today")
        val candidateList = mutableListOf(
            CandidateEmail("id_1", meta1),
            CandidateEmail("id_2", meta2)
        )

        val ticked = setOf("id_1")
        val filtered = GmailApiClient.discardUnticked(candidateList, ticked)

        assertEquals(1, filtered.size)
        assertEquals("id_1", filtered[0].id)
        assertFalse(filtered.any { it.id == "id_2" })
    }

    @Test
    fun testDisconnectClearsMemory() {
        GmailApiClient.setToken("mock_token")
        assertTrue(GmailApiClient.isConnected())

        GmailApiClient.disconnect()
        assertFalse(GmailApiClient.isConnected())
        assertNull(GmailApiClient.accessToken)
    }
}
