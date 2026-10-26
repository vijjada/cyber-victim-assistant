package com.cybervictim.assistant.email

import android.util.Base64
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * On-device RFC 822 / MIME parser.
 * Strict safety invariants:
 * 1. Never renders HTML in a WebView; strips HTML to safe plain text.
 * 2. Never downloads, executes, or previews attachments; extracts metadata only.
 * 3. Never follows or pre-fetches any links.
 */
object EmlParser {

    private val SHORTENER_DOMAINS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "is.gd", "buff.ly", "cutt.ly",
        "ow.ly", "goo.gl", "rebrand.ly", "shorturl.at", "bl.ink"
    )

    private val RISKY_EXTENSIONS = setOf(
        "exe", "html", "htm", "iso", "lnk", "js", "scr", "zip",
        "docm", "xlsm", "pptm", "vbs", "bat", "cmd", "pif", "wsf", "hta", "jar"
    )

    private val DATE_FORMATS = listOf(
        "EEE, d MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "d MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd HH:mm:ss"
    )

    fun parse(rawText: String, id: String = UUID.randomUUID().toString()): EmailContent {
        return parse(rawText.toByteArray(StandardCharsets.UTF_8), id)
    }

    fun parse(rawBytes: ByteArray, id: String = UUID.randomUUID().toString()): EmailContent {
        val sha256 = computeSha256(rawBytes)
        val fullText = String(rawBytes, StandardCharsets.UTF_8)

        // Split headers from body (separated by double newline)
        val headerSplitIndex = findHeaderBodySeparator(fullText)
        val headerBlock: String
        val bodyBlock: String

        if (headerSplitIndex >= 0) {
            headerBlock = fullText.substring(0, headerSplitIndex)
            bodyBlock = fullText.substring(headerSplitIndex).trimStart('\r', '\n')
        } else {
            headerBlock = fullText
            bodyBlock = ""
        }

        val rawHeaders = parseHeaders(headerBlock)
        val from = decodeRfc2047(rawHeaders["from"] ?: "Unknown Sender")
        val replyTo = rawHeaders["reply-to"]?.let { decodeRfc2047(it) }
        val returnPath = rawHeaders["return-path"]
        val to = rawHeaders["to"]?.let { decodeRfc2047(it) }
        val subject = decodeRfc2047(rawHeaders["subject"] ?: "(No Subject)")
        val dateStr = rawHeaders["date"] ?: ""
        val messageId = rawHeaders["message-id"]
        val authResults = rawHeaders["authentication-results"]
        val receivedList = parseReceivedHeaders(headerBlock)
        val contentType = rawHeaders["content-type"] ?: "text/plain"

        val timestamp = parseDateToMillis(dateStr)

        val metadata = EmailMetadata(
            id = id,
            from = from,
            replyTo = replyTo,
            returnPath = returnPath,
            to = to,
            subject = subject,
            dateStr = dateStr,
            timestampMillis = timestamp,
            messageIdHeader = messageId,
            authResults = authResults,
            receivedHeaders = receivedList
        )

        // Parse MIME structure
        val parsedBodyAndAttachments = parseMimeParts(contentType, bodyBlock)

        // Extract and analyze links safely
        val links = extractLinks(parsedBodyAndAttachments.plainText, parsedBodyAndAttachments.htmlLinkPairs)

        val hasQrMention = parsedBodyAndAttachments.plainText.contains("QR", ignoreCase = true) ||
                subject.contains("QR", ignoreCase = true) ||
                parsedBodyAndAttachments.attachments.any { it.fileName.contains("qr", ignoreCase = true) }

        return EmailContent(
            metadata = metadata,
            plainTextBody = parsedBodyAndAttachments.plainText.trim(),
            extractedLinks = links,
            attachments = parsedBodyAndAttachments.attachments,
            rawEmlBytes = rawBytes,
            rawEmlSha256 = sha256,
            hasQrCodeMention = hasQrMention
        )
    }

    fun parseFromPastedText(text: String): EmailContent {
        return parse(text.toByteArray(StandardCharsets.UTF_8))
    }

    private fun findHeaderBodySeparator(text: String): Int {
        val crlfcrlf = text.indexOf("\r\n\r\n")
        val lflf = text.indexOf("\n\n")
        return when {
            crlfcrlf >= 0 && lflf >= 0 -> minOf(crlfcrlf, lflf)
            crlfcrlf >= 0 -> crlfcrlf
            lflf >= 0 -> lflf
            else -> -1
        }
    }

    private fun parseHeaders(headerBlock: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val lines = headerBlock.lines()
        var currentKey: String? = null
        var currentValue = StringBuilder()

        for (line in lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                // Continuation of previous header (folding)
                if (currentKey != null) {
                    currentValue.append(" ").append(line.trim())
                }
            } else {
                if (currentKey != null) {
                    val keyLower = currentKey.lowercase(Locale.ROOT)
                    if (!map.containsKey(keyLower)) {
                        map[keyLower] = currentValue.toString().trim()
                    }
                }
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    currentKey = line.substring(0, colonIdx).trim()
                    currentValue = StringBuilder(line.substring(colonIdx + 1).trim())
                } else {
                    currentKey = null
                }
            }
        }
        if (currentKey != null) {
            val keyLower = currentKey.lowercase(Locale.ROOT)
            if (!map.containsKey(keyLower)) {
                map[keyLower] = currentValue.toString().trim()
            }
        }
        return map
    }

    private fun parseReceivedHeaders(headerBlock: String): List<String> {
        val list = mutableListOf<String>()
        val lines = headerBlock.lines()
        var capturingReceived = false
        var currentReceived = StringBuilder()

        for (line in lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (capturingReceived) {
                    currentReceived.append(" ").append(line.trim())
                }
            } else {
                if (capturingReceived) {
                    list.add(currentReceived.toString().trim())
                    currentReceived = StringBuilder()
                    capturingReceived = false
                }
                if (line.startsWith("Received:", ignoreCase = true)) {
                    capturingReceived = true
                    currentReceived.append(line.substringAfter(':').trim())
                }
            }
        }
        if (capturingReceived) {
            list.add(currentReceived.toString().trim())
        }
        return list
    }

    private data class ParsedMimeResult(
        val plainText: String,
        val htmlLinkPairs: List<Pair<String, String>>, // (displayText, href)
        val attachments: List<AttachmentMetadata>
    )

    private fun parseMimeParts(contentTypeHeader: String, bodyText: String): ParsedMimeResult {
        val boundary = extractBoundary(contentTypeHeader)
        val attachments = mutableListOf<AttachmentMetadata>()
        val plainTextBuilder = StringBuilder()
        val htmlLinkPairs = mutableListOf<Pair<String, String>>()

        if (boundary != null) {
            val parts = bodyText.split("--$boundary")
            for (part in parts) {
                val trimmedPart = part.trim()
                if (trimmedPart.isEmpty() || trimmedPart.startsWith("--")) continue

                val separatorIdx = findHeaderBodySeparator(trimmedPart)
                if (separatorIdx < 0) continue

                val partHeaderBlock = trimmedPart.substring(0, separatorIdx)
                val partBody = trimmedPart.substring(separatorIdx).trimStart('\r', '\n')
                val partHeaders = parseHeaders(partHeaderBlock)

                val partContentType = partHeaders["content-type"] ?: "text/plain"
                val partDisposition = partHeaders["content-disposition"] ?: ""
                val partEncoding = partHeaders["content-transfer-encoding"] ?: "7bit"

                val filename = extractFilename(partDisposition, partContentType)

                if (filename != null || partDisposition.startsWith("attachment", ignoreCase = true)) {
                    // It's an attachment! Record metadata without downloading or previewing
                    val safeName = filename ?: "attachment_${attachments.size + 1}"
                    val mime = partContentType.substringBefore(';').trim().lowercase(Locale.ROOT)
                    val ext = safeName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    val isRisky = ext in RISKY_EXTENSIONS || mime.contains("javascript") || mime.contains("html")
                    val approxSize = computeApproximateSize(partBody, partEncoding)

                    attachments.add(
                        AttachmentMetadata(
                            fileName = safeName,
                            mimeType = mime,
                            sizeBytes = approxSize,
                            isRiskyType = isRisky
                        )
                    )
                } else if (partContentType.contains("multipart/", ignoreCase = true)) {
                    val nestedResult = parseMimeParts(partContentType, partBody)
                    plainTextBuilder.append(nestedResult.plainText).append("\n")
                    htmlLinkPairs.addAll(nestedResult.htmlLinkPairs)
                    attachments.addAll(nestedResult.attachments)
                } else {
                    val decodedBody = decodeBody(partBody, partEncoding)
                    if (partContentType.contains("text/html", ignoreCase = true)) {
                        val (safeText, extractedPairs) = extractSafeTextAndLinksFromHtml(decodedBody)
                        if (plainTextBuilder.isEmpty()) {
                            plainTextBuilder.append(safeText)
                        }
                        htmlLinkPairs.addAll(extractedPairs)
                    } else {
                        plainTextBuilder.append(decodedBody).append("\n")
                    }
                }
            }
        } else {
            // Single part email
            if (contentTypeHeader.contains("text/html", ignoreCase = true)) {
                val (safeText, extractedPairs) = extractSafeTextAndLinksFromHtml(bodyText)
                plainTextBuilder.append(safeText)
                htmlLinkPairs.addAll(extractedPairs)
            } else {
                plainTextBuilder.append(bodyText)
            }
        }

        return ParsedMimeResult(
            plainText = plainTextBuilder.toString().trim(),
            htmlLinkPairs = htmlLinkPairs,
            attachments = attachments
        )
    }

    private fun extractBoundary(contentType: String): String? {
        val pattern = Pattern.compile("boundary=[\"']?([^\"';\\s]+)[\"']?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(contentType)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractFilename(disposition: String, contentType: String): String? {
        val dispPattern = Pattern.compile("filename=[\"']?([^\"';\\r\\n]+)[\"']?", Pattern.CASE_INSENSITIVE)
        val dispMatcher = dispPattern.matcher(disposition)
        if (dispMatcher.find()) return decodeRfc2047(dispMatcher.group(1)!!.trim())

        val typePattern = Pattern.compile("name=[\"']?([^\"';\\r\\n]+)[\"']?", Pattern.CASE_INSENSITIVE)
        val typeMatcher = typePattern.matcher(contentType)
        if (typeMatcher.find()) return decodeRfc2047(typeMatcher.group(1)!!.trim())

        return null
    }

    fun safeBase64Decode(str: String): ByteArray {
        val clean = str.replace("\r", "").replace("\n", "").replace(" ", "")
        return try {
            java.util.Base64.getDecoder().decode(clean)
        } catch (_: Throwable) {
            try {
                Base64.decode(clean, Base64.DEFAULT)
            } catch (_: Throwable) {
                ByteArray(0)
            }
        }
    }

    private fun decodeBody(body: String, encoding: String): String {
        return when (encoding.lowercase(Locale.ROOT).trim()) {
            "base64" -> {
                try {
                    val bytes = safeBase64Decode(body)
                    String(bytes, StandardCharsets.UTF_8)
                } catch (_: Exception) {
                    body
                }
            }
            "quoted-printable" -> decodeQuotedPrintable(body)
            else -> body
        }
    }

    private fun decodeQuotedPrintable(input: String): String {
        val out = StringBuilder()
        val reader = BufferedReader(StringReader(input))
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            val l = line!!
            var i = 0
            val len = l.length
            while (i < len) {
                val c = l[i]
                if (c == '=' && i + 2 < len) {
                    val hex = l.substring(i + 1, i + 3)
                    try {
                        val byteVal = hex.toInt(16)
                        out.append(byteVal.toChar())
                        i += 3
                        continue
                    } catch (_: Exception) {}
                } else if (c == '=' && i == len - 1) {
                    // Soft line break
                    i++
                    continue
                }
                out.append(c)
                i++
            }
            out.append("\n")
        }
        return out.toString().trimEnd('\n')
    }

    fun decodeRfc2047(input: String): String {
        if (!input.contains("=?")) return input
        val pattern = Pattern.compile("=\\?([^?]+)\\?([bBqQ])\\?([^?]+)\\?=")
        val matcher = pattern.matcher(input)
        val sb = StringBuffer()

        while (matcher.find()) {
            val charsetName = matcher.group(1) ?: "UTF-8"
            val encoding = matcher.group(2) ?: "B"
            val encodedText = matcher.group(3) ?: ""
            var replacement = matcher.group(0)

            try {
                val charset = try { Charset.forName(charsetName) } catch (_: Exception) { StandardCharsets.UTF_8 }
                val decodedBytes = if (encoding.equals("B", ignoreCase = true)) {
                    safeBase64Decode(encodedText)
                } else {
                    decodeQuotedPrintableBytes(encodedText)
                }
                replacement = String(decodedBytes, charset)
            } catch (_: Exception) {}

            matcher.appendReplacement(sb, MatcherQuoteReplacement(replacement ?: ""))
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

    private fun MatcherQuoteReplacement(s: String): String {
        return s.replace("\\", "\\\\").replace("$", "\\$")
    }

    private fun decodeQuotedPrintableBytes(text: String): ByteArray {
        val bytes = mutableListOf<Byte>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '=' && i + 2 < text.length) {
                try {
                    val b = text.substring(i + 1, i + 3).toInt(16).toByte()
                    bytes.add(b)
                    i += 3
                    continue
                } catch (_: Exception) {}
            } else if (c == '_') {
                bytes.add(' '.code.toByte())
            } else {
                bytes.add(c.code.toByte())
            }
            i++
        }
        return bytes.toByteArray()
    }

    private fun extractSafeTextAndLinksFromHtml(html: String): Pair<String, List<Pair<String, String>>> {
        val linkPairs = mutableListOf<Pair<String, String>>()

        // Extract <a> tags: <a href="url">text</a>
        val linkPattern = Pattern.compile("<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = linkPattern.matcher(html)
        while (matcher.find()) {
            val href = matcher.group(1)?.trim() ?: ""
            val rawText = matcher.group(2)?.trim() ?: ""
            val cleanText = stripHtml(rawText).trim()
            if (href.isNotEmpty()) {
                linkPairs.add(cleanText to href)
            }
        }

        // Convert HTML to safe plain text (strip tags)
        val safeText = stripHtml(html)
        return safeText to linkPairs
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\n\\s+\n"), "\n\n")
            .trim()
    }

    private fun extractLinks(plainText: String, htmlLinks: List<Pair<String, String>>): List<EmailLink> {
        val result = mutableListOf<EmailLink>()
        val seenUrls = mutableSetOf<String>()

        // 1. Add html link pairs
        for ((disp, target) in htmlLinks) {
            if (target.startsWith("http://", ignoreCase = true) || target.startsWith("https://", ignoreCase = true)) {
                val isShort = isShortenedUrl(target)
                val isIp = isIpAddressUrl(target)
                val isMismatch = isLinkMismatch(disp, target)
                result.add(EmailLink(disp.ifEmpty { target }, target, isMismatch, isIp, isShort))
                seenUrls.add(target.lowercase(Locale.ROOT))
            }
        }

        // 2. Scan plain text for raw URLs
        val urlPattern = Pattern.compile("https?://[a-zA-Z0-9.-]+(?:/[^\\s<>\"']*)?", Pattern.CASE_INSENSITIVE)
        val matcher = urlPattern.matcher(plainText)
        while (matcher.find()) {
            val url = matcher.group()
            if (!seenUrls.contains(url.lowercase(Locale.ROOT))) {
                val isShort = isShortenedUrl(url)
                val isIp = isIpAddressUrl(url)
                result.add(EmailLink(url, url, isMismatch = false, isIpBased = isIp, isShortened = isShort))
                seenUrls.add(url.lowercase(Locale.ROOT))
            }
        }

        return result
    }

    private fun isLinkMismatch(displayText: String, targetUrl: String): Boolean {
        val dispLower = displayText.lowercase(Locale.ROOT).trim()
        val targetLower = targetUrl.lowercase(Locale.ROOT).trim()

        // If display text looks like a domain or URL
        if (dispLower.startsWith("http://") || dispLower.startsWith("https://") || dispLower.contains(".com") || dispLower.contains(".in") || dispLower.contains(".org") || dispLower.contains(".co.")) {
            val dispDomain = extractDomainFromText(dispLower)
            val targetDomain = extractDomainFromText(targetLower)
            if (dispDomain != null && targetDomain != null && dispDomain != targetDomain) {
                return true
            }
        }
        return false
    }

    private fun isShortenedUrl(url: String): Boolean {
        val domain = extractDomainFromText(url) ?: return false
        return domain in SHORTENER_DOMAINS
    }

    private fun isIpAddressUrl(url: String): Boolean {
        val domain = extractDomainFromText(url) ?: return false
        return domain.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))
    }

    fun extractDomainFromText(text: String): String? {
        val clean = text.removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore(':')
        return clean.trim().takeIf { it.isNotEmpty() }
    }

    private fun computeApproximateSize(body: String, encoding: String): Long {
        val len = body.length.toLong()
        return when (encoding.lowercase(Locale.ROOT).trim()) {
            "base64" -> (len * 3) / 4
            else -> len
        }
    }

    fun computeSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        val clean = dateStr.replace(Regex("\\s*\\([^)]*\\)\\s*"), "").trim()
        for (pattern in DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(clean)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }
}
