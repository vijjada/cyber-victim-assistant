package com.cybervictim.assistant.voice

import android.os.Handler
import android.os.Looper
import com.cybervictim.assistant.ai.GeminiClient
import com.cybervictim.assistant.exposure.ExposureApi
import com.cybervictim.assistant.portal.StepOwner
import com.cybervictim.assistant.ui.L10n
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class VoiceGuideMessage(
    val speaker: String, // "user" or "assistant"
    val text: String
)

/**
 * Client for Voice Guide next-step guidance.
 * Sends transcript, languageTag, and ScreenDescriptor (names only, no user values)
 * to backend /voice-guide.
 * Falls back to on-device GeminiClient, then to grounded L10n screen hints.
 */
object VoiceGuideClient {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun requestGuidance(
        transcript: String,
        languageTag: String,
        descriptor: ScreenDescriptor,
        history: List<VoiceGuideMessage>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val baseUrl = ExposureApi.baseUrl()
        if (baseUrl.isNotEmpty()) {
            Thread {
                try {
                    val url = URL("$baseUrl/voice-guide")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.connectTimeout = 5000
                    conn.readTimeout = 8000
                    conn.setRequestProperty("Content-Type", "application/json")

                    val historyArray = JSONArray()
                    history.takeLast(3).forEach {
                        historyArray.put(JSONObject().apply {
                            put("speaker", it.speaker)
                            put("text", it.text)
                        })
                    }

                    val payload = JSONObject().apply {
                        put("transcript", transcript)
                        put("languageTag", languageTag)
                        put("screen", descriptor.toJson())
                        put("history", historyArray)
                    }

                    conn.outputStream.use { it.write(payload.toString().toByteArray()) }

                    val code = conn.responseCode
                    if (code in 200..299) {
                        val body = conn.inputStream.bufferedReader().readText()
                        conn.disconnect()
                        val json = JSONObject(body)
                        val nextStep = json.optString("nextStep", "").trim()
                        if (nextStep.isNotEmpty()) {
                            mainHandler.post { onSuccess(nextStep) }
                            return@Thread
                        }
                    }
                    conn.disconnect()
                } catch (_: Throwable) {
                    // Fall through to on-device Gemini fallback
                }

                // If backend was unreachable or returned empty, run local Gemini fallback
                runOnDeviceFallback(transcript, languageTag, descriptor, history, onSuccess, onError)
            }.start()
        } else {
            // Direct on-device fallback when backend is not configured
            runOnDeviceFallback(transcript, languageTag, descriptor, history, onSuccess, onError)
        }
    }

    private fun runOnDeviceFallback(
        transcript: String,
        languageTag: String,
        descriptor: ScreenDescriptor,
        history: List<VoiceGuideMessage>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val geminiHistory = history.takeLast(3).map {
            (it.speaker == "user") to it.text
        }

        val prompt = buildString {
            appendLine("The victim is asking for voice assistance while filing a cybercrime report.")
            appendLine("Current Screen: ${descriptor.title} (ID: ${descriptor.screenId})")
            if (descriptor.stepIndex >= 0) {
                appendLine("Step ${descriptor.stepIndex}: ${descriptor.stepTitle}")
            }
            if (descriptor.buttons.isNotEmpty()) {
                appendLine("Available buttons: ${descriptor.buttons.joinToString(", ")}")
            }
            if (descriptor.fields.isNotEmpty()) {
                appendLine("Available fields: ${descriptor.fields.joinToString(", ")}")
            }
            when (descriptor.owner) {
                StepOwner.HUMAN -> appendLine("IMPORTANT: This is a manual step. The user MUST enter OTP / Captcha / login themselves.")
                StepOwner.VICTIM_LOCK -> appendLine("IMPORTANT: This is the final locked submit step. The user MUST tap Submit themselves.")
                StepOwner.AI_AUTO -> appendLine("Guide the user to the single next tap or action.")
            }
            appendLine("Target Language Tag: $languageTag")
            appendLine("Victim said: \"$transcript\"")
            appendLine("Instruction: Speak back exactly ONE clear next step in the victim's language ($languageTag). The AI only guides; the user does all tapping. Keep it under 2 sentences. Strictly no emojis.")
        }

        GeminiClient.chat(
            history = geminiHistory,
            userMessage = prompt,
            onReply = { reply ->
                val cleaned = reply.replace(Regex("[\uD83C-\uDBFF\uDC00-\uDFFF]+"), "").trim()
                if (cleaned.isNotBlank()) {
                    onSuccess(cleaned)
                } else {
                    onSuccess(fallbackHint(descriptor))
                }
            },
            onError = {
                // Grounded local L10n fallback
                onSuccess(fallbackHint(descriptor))
            }
        )
    }

    private fun fallbackHint(descriptor: ScreenDescriptor): String {
        return if (descriptor.hintKey.isNotEmpty()) {
            L10n.t(descriptor.hintKey)
        } else {
            L10n.t("voice_guide_sub")
        }
    }
}
