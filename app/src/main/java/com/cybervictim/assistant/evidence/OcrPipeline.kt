package com.cybervictim.assistant.evidence

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.cybervictim.assistant.ai.CaseExtractor

/**
 * Phase 4 (route A): fully on-device OCR via ML Kit. The image never leaves
 * the phone; OCR text feeds the regex rules and then the (already existing)
 * Gemini text pass if the user chooses.
 */
object OcrPipeline {

    /** Downsampled decode to keep OCR fast and memory low. */
    fun decodeSampled(ctx: Context, uri: Uri, maxSide: Int = 1600): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null
        var sample = 1
        val maxOrig = maxOf(opts.outWidth, opts.outHeight)
        while (maxOrig / sample > maxSide) sample *= 2
        val load = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
        return ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, load) }
    }

    /** Blocks until OCR completes (worker thread); returns plain text. */
    fun recognizeText(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = com.google.android.gms.tasks.Tasks.await(
            recognizer.process(image), 30_000L, java.util.concurrent.TimeUnit.MILLISECONDS
        )
        recognizer.close()
        return result.text ?: ""
    }

    /**------ Rule-based fact mining (pure regex, no network) ------*/
    data class RuleFact(val type: String, val value: String)

    private val UPI_SUFFIXES = listOf(
        "ybl", "oksbi", "okhdfcbank", "okaxis", "okicici", "paytm", "apl", "axl", "abfspay",
        "icici", "axisbank", "hdfcbank", "sbi", "fbl", "ybl.", "indianbank", "atlas", "stanaxis",
        "ptsbi", "ptaxis", "pthdfcbank", "ptics", "ptaxisbank", "upi", "aibl", "abfl", "yesbank",
    )

    fun extract(txtRaw: String): List<RuleFact> {
        val txt = txtRaw.replace('\u00a0', ' ')
        val out = LinkedHashMap<String, RuleFact>()

        // phone numbers
        Regex("(?:\\+91[-\\s]?|0)?[6-9]\\d{9}\\b").findAll(txt).forEach { r ->
            out["phone:" + r.value] = RuleFact("phone", r.value)
        }
        // amounts (₹ / Rs / INR)
        Regex("(?:₹|rs\\.?|inr)\\s?\\d{1,3}(?:[,\\s]\\d{2,3})*(?:\\.\\d{1,2})?|\\d{2,7}\\.\\d{1,2}\\s?(?:₹|debited|credited)", RegexOption.IGNORE_CASE)
            .findAll(txt).forEach { r ->
                val v = r.value.trim()
                if (v.any { it.isDigit() }) out["amount:" + v] = RuleFact("amount", v)
            }
        // UPI ids: word@suffix (known-bank suffixes weighted higher)
        Regex("\\b[a-zA-Z][a-zA-Z0-9._\\-]{2,60}@[a-zA-Z][a-zA-Z0-9]{2,24}\\b").findAll(txt).forEach { r ->
            val v = r.value
            val suffix = v.substringAfterLast('@').lowercase()
            val known = UPI_SUFFIXES.any { suffix.startsWith(it) || suffix.endsWith(it) }
            if (known) out["upi:" + v] = RuleFact("upi_id", v)
            else out.putIfAbsent("maybe_upi:" + v, RuleFact("other", "$v (possible UPI id — confirm)"))
        }
        // transaction refs
        Regex("(?i)\\b(?:utr|txn|ref(?:\\.?\\s*(?:no\\.?|number))?|upi:\\s*)(?:ref\\b[:=\\s]*)?([a-z0-9]{9,24})\\b")
            .findAll(txt).forEach { r ->
                r.groups[1]?.value?.let { out["txn:" + r.value] = RuleFact("txn_ref", it) }
            }
        // timestamps (plain — interpretation later)
        Regex("\\b\\d{2}[:/.]\\d{2}(?::\\d{2})?\\b(?:\\s?(?:am|pm|AM|PM))?(?=\\b)|\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b", )
            .findAll(txt).forEach { r ->
                out["ts:" + r.value] = RuleFact("timestamp", r.value)
            }
        return out.values.toList()
    }

    /** One-shot helper used by the UI thread-free path: OCR + rules. */
    fun processImage(ctx: Context, uri: Uri): Pair<String, List<CaseExtractor.Fact>> {
        val bmp = decodeSampled(ctx, uri) ?: return "" to emptyList()
        val text = recognizeText(bmp)
        bmp.recycle()
        val rules = extract(text).map { CaseExtractor.Fact(it.type, it.value) }
        return text to rules
    }
}
