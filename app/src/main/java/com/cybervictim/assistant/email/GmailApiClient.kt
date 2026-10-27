package com.cybervictim.assistant.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

interface GmailTransport {
    fun executeGet(url: String, headers: Map<String, String>): Pair<Int, String>
    fun executePost(url: String, headers: Map<String, String>, body: String): Pair<Int, String>
}

object DefaultGmailTransport : GmailTransport {
    override fun executeGet(url: String, headers: Map<String, String>): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return Pair(code, responseBody)
    }

    override fun executePost(url: String, headers: Map<String, String>, body: String): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.doOutput = true
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

        OutputStreamWriter(conn.outputStream, "UTF-8").use {
            it.write(body)
            it.flush()
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return Pair(code, responseBody)
    }
}

object GmailOAuthHelper {
    const val OAUTH_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    const val OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token"
    const val OAUTH_REVOKE_URL = "https://oauth2.googleapis.com/revoke"
    const val SCOPE_GMAIL_READONLY = "https://www.googleapis.com/auth/gmail.readonly"
    const val DEFAULT_REDIRECT_URI = "com.cybervictim.assistant:/oauth2redirect"

    private val secureRandom = SecureRandom()

    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val hash = sha256.digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    fun generateState(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun buildAuthUrl(
        clientId: String,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        codeChallenge: String,
        state: String
    ): String {
        val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
        val encodedScope = URLEncoder.encode(SCOPE_GMAIL_READONLY, "UTF-8")
        val encodedChallenge = URLEncoder.encode(codeChallenge, "UTF-8")
        val encodedState = URLEncoder.encode(state, "UTF-8")
        val encodedClientId = URLEncoder.encode(clientId, "UTF-8")

        return "$OAUTH_AUTH_URL?client_id=$encodedClientId&redirect_uri=$encodedRedirect&response_type=code&scope=$encodedScope&code_challenge=$encodedChallenge&code_challenge_method=S256&state=$encodedState&prompt=consent"
    }

    fun exchangeCodeForToken(
        clientId: String,
        redirectUri: String,
        code: String,
        codeVerifier: String,
        transport: GmailTransport = DefaultGmailTransport
    ): Result<String> {
        val params = listOf(
            "client_id" to clientId,
            "code" to code,
            "code_verifier" to codeVerifier,
            "grant_type" to "authorization_code",
            "redirect_uri" to redirectUri
        ).joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

        val headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
        val (codeNum, responseBody) = transport.executePost(OAUTH_TOKEN_URL, headers, params)

        if (codeNum in 200..299) {
            val json = JSONObject(responseBody)
            val accessToken = json.optString("access_token")
            return if (accessToken.isNotBlank()) {
                Result.success(accessToken)
            } else {
                Result.failure(IllegalStateException("No access_token found in token response"))
            }
        }
        return Result.failure(IllegalStateException("Token exchange failed ($codeNum): $responseBody"))
    }

    fun revokeToken(token: String, transport: GmailTransport = DefaultGmailTransport): Boolean {
        return try {
            val body = "token=" + URLEncoder.encode(token, "UTF-8")
            val headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
            val (code, _) = transport.executePost(OAUTH_REVOKE_URL, headers, body)
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }
}

data class CandidateEmail(
    val id: String,
    val metadata: EmailMetadata,
    var isSelected: Boolean = false
)

object GmailApiClient {
    const val MAX_WINDOW_SECONDS = 7 * 24 * 3600L // 7 days hard cap

    // Access token is held in-memory ONLY. Never persisted to disk, room, or shared preferences.
    var accessToken: String? = null
        private set

    var pendingCodeVerifier: String? = null
    var pendingState: String? = null

    fun setToken(token: String) {
        accessToken = token
    }

    fun isConnected(): Boolean = !accessToken.isNullOrBlank()

    fun disconnect(transport: GmailTransport = DefaultGmailTransport) {
        val current = accessToken
        accessToken = null
        pendingCodeVerifier = null
        pendingState = null
        if (current != null) {
            try {
                GmailOAuthHelper.revokeToken(current, transport)
            } catch (_: Exception) {
                // best effort revocation
            }
        }
    }

    fun validateWindow(startEpochSec: Long, endEpochSec: Long): Result<Unit> {
        if (startEpochSec > endEpochSec) {
            return Result.failure(IllegalArgumentException("Start time cannot be after end time."))
        }
        val duration = endEpochSec - startEpochSec
        if (duration > MAX_WINDOW_SECONDS) {
            return Result.failure(IllegalArgumentException("Search window cannot exceed 7 days (requested ${(duration / 86400.0).toInt()} days)."))
        }
        return Result.success(Unit)
    }

    suspend fun listMetadataOnly(
        startEpochSec: Long,
        endEpochSec: Long,
        transport: GmailTransport = DefaultGmailTransport
    ): Result<List<CandidateEmail>> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(IllegalStateException("Gmail not connected"))

        val validation = validateWindow(startEpochSec, endEpochSec)
        if (validation.isFailure) return@withContext Result.failure(validation.exceptionOrNull() ?: IllegalArgumentException("Invalid window"))

        val query = "after:$startEpochSec before:$endEpochSec"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val listUrl = "https://gmail.googleapis.com/gmail/v1/users/me/messages?q=$encodedQuery&includeSpamTrash=true&maxResults=50"
        val headers = mapOf("Authorization" to "Bearer $token")

        val (listCode, listBody) = transport.executeGet(listUrl, headers)
        if (listCode !in 200..299) {
            return@withContext Result.failure(IllegalStateException("Gmail message query failed ($listCode)"))
        }

        val json = JSONObject(listBody)
        val messagesArray = json.optJSONArray("messages") ?: JSONArray()
        val candidates = mutableListOf<CandidateEmail>()

        val metadataHeaderQuery = listOf(
            "From", "Reply-To", "Return-Path", "To", "Subject",
            "Date", "Message-ID", "Authentication-Results", "Received"
        ).joinToString("") { "&metadataHeaders=${URLEncoder.encode(it, "UTF-8")}" }

        for (i in 0 until minOf(50, messagesArray.length())) {
            val msgObj = messagesArray.getJSONObject(i)
            val id = msgObj.getString("id")

            val msgUrl = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=metadata$metadataHeaderQuery"
            val (msgCode, msgBody) = transport.executeGet(msgUrl, headers)
            if (msgCode in 200..299) {
                val parsedMetadata = parseMetadataJson(id, JSONObject(msgBody))
                val (score, reason) = PhishingAnalyzer.quickScoreMetadata(parsedMetadata)
                val enriched = parsedMetadata.copy(quickScore = score, quickSuspicionReason = reason)
                candidates.add(CandidateEmail(id, enriched))
            }
        }

        // Sort descending by suspicion score (highest risk first)
        candidates.sortByDescending { it.metadata.quickScore }
        Result.success(candidates)
    }

    suspend fun fetchRawMessage(
        messageId: String,
        transport: GmailTransport = DefaultGmailTransport
    ): Result<String> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(IllegalStateException("Gmail not connected"))
        val rawUrl = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId?format=raw"
        val headers = mapOf("Authorization" to "Bearer $token")
        val (code, body) = transport.executeGet(rawUrl, headers)
        if (code !in 200..299) {
            return@withContext Result.failure(IllegalStateException("Failed to fetch message ($code)"))
        }

        val json = JSONObject(body)
        val rawB64 = json.optString("raw")
        if (rawB64.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Empty raw email content"))
        }

        val decodedBytes = try {
            Base64.getUrlDecoder().decode(rawB64)
        } catch (_: Exception) {
            android.util.Base64.decode(rawB64, android.util.Base64.URL_SAFE)
        }

        Result.success(String(decodedBytes, Charsets.UTF_8))
    }

    /**
     * Drops all unticked candidate metadata from memory immediately upon import selection.
     */
    fun discardUnticked(candidates: MutableList<CandidateEmail>, tickedIds: Set<String>): List<CandidateEmail> {
        candidates.retainAll { tickedIds.contains(it.id) }
        return candidates
    }

    private fun parseMetadataJson(id: String, json: JSONObject): EmailMetadata {
        val payload = json.optJSONObject("payload")
        val headers = payload?.optJSONArray("headers") ?: JSONArray()
        var from = ""
        var replyTo: String? = null
        var returnPath: String? = null
        var to: String? = null
        var subject = "(No Subject)"
        var dateStr = ""
        var msgId: String? = null
        var authResults: String? = null
        val received = mutableListOf<String>()

        for (i in 0 until headers.length()) {
            val h = headers.getJSONObject(i)
            val name = h.optString("name", "")
            val value = h.optString("value", "")
            when (name.lowercase()) {
                "from" -> from = EmlParser.decodeRfc2047(value)
                "reply-to" -> replyTo = EmlParser.decodeRfc2047(value)
                "return-path" -> returnPath = value
                "to" -> to = EmlParser.decodeRfc2047(value)
                "subject" -> subject = EmlParser.decodeRfc2047(value)
                "date" -> dateStr = value
                "message-id" -> msgId = value
                "authentication-results" -> authResults = value
                "received" -> received.add(value)
            }
        }

        val internalDate = json.optLong("internalDate", System.currentTimeMillis())

        return EmailMetadata(
            id = id,
            from = from,
            replyTo = replyTo,
            returnPath = returnPath,
            to = to,
            subject = subject,
            dateStr = dateStr,
            timestampMillis = internalDate,
            messageIdHeader = msgId,
            authResults = authResults,
            receivedHeaders = received
        )
    }
}
