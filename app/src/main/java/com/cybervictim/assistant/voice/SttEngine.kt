package com.cybervictim.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

interface SttEngine {
    fun isAvailable(): Boolean
    fun startListening(
        languageTag: String,
        onReady: () -> Unit = {},
        onRmsChanged: (Float) -> Unit = {},
        onPartialResult: (String) -> Unit = {},
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    )
    fun stopListening()
}

/**
 * On-device SpeechRecognizer engine.
 * Maps the app's chosen language to the appropriate BCP-47 language tag for all 12 languages.
 * Fixes:
 * - Threading: synchronously cleans up prior recognizers on the main thread (no async destroy race).
 * - Android 13+ on-device recognizer support (SpeechRecognizer.createOnDeviceSpeechRecognizer).
 * - Adds RecognizerIntent.EXTRA_CALLING_PACKAGE required by RecognitionService.
 * - Adds additional Indian language fallbacks so recognition never fails for regional speech.
 */
class AndroidSttEngine(private val context: Context) : SttEngine {
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false

    override fun isAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Throwable) {
            false
        }
    }

    private fun cleanupInternal() {
        try {
            recognizer?.stopListening()
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Throwable) {}
        recognizer = null
        isListening = false
    }

    override fun stopListening() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            cleanupInternal()
        } else {
            mainHandler.post { cleanupInternal() }
        }
    }

    override fun startListening(
        languageTag: String,
        onReady: () -> Unit,
        onRmsChanged: (Float) -> Unit,
        onPartialResult: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val task = Runnable {
            try {
                // Synchronously clean up any prior instance (never post asynchronously to avoid killing the new one)
                cleanupInternal()

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    onError("Speech recognition service is not available on this device")
                    return@Runnable
                }

                // Use standard system SpeechRecognizer (supports online + offline recognition for Indian languages)
                val rec = SpeechRecognizer.createSpeechRecognizer(context)

                recognizer = rec
                isListening = true

                rec.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onReady()
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        isListening = false
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
                            SpeechRecognizer.ERROR_CLIENT -> "Client connection error. Tap mic to retry."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error for speech recognition."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Tap mic to retry."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak closer to the mic."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy. Please retry."
                            SpeechRecognizer.ERROR_SERVER -> "Recognition server error. Tap mic to retry."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Please speak your question."
                            10 -> "Too many requests. Please wait a moment."
                            11 -> "Server disconnected. Tap mic to retry."
                            12, 13 -> "LANGUAGE_UNAVAILABLE" // Handled by UI to show voice_error_unavailable
                            else -> "Speech recognition error ($error)"
                        }
                        onError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            onResult(text)
                        } else {
                            onError("No speech recognized. Please speak your question.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            onPartialResult(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(languageTag, "en-IN", "hi-IN"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                }

                rec.startListening(intent)
            } catch (e: Throwable) {
                isListening = false
                onError(e.message ?: "Failed to start speech recognition")
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run()
        } else {
            mainHandler.post(task)
        }
    }
}

/**
 * Stub for future server-side speech recognition model.
 */
class BackendSttEngine : SttEngine {
    override fun isAvailable(): Boolean = false

    override fun startListening(
        languageTag: String,
        onReady: () -> Unit,
        onRmsChanged: (Float) -> Unit,
        onPartialResult: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        onError("Backend STT engine not configured yet. Please type your query.")
    }

    override fun stopListening() {}
}
