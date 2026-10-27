package com.cybervictim.assistant.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * TextToSpeech manager for voice narration in CyBridge.
 * Reads speech rate from cva_settings (speech_rate) and speaks in the chosen language.
 */
object TtsManager {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingTask: (() -> Unit)? = null
    private var currentRate = 0.9f

    fun init(context: Context, onReady: (() -> Unit)? = null) {
        if (tts != null && isInitialized) {
            onReady?.invoke()
            return
        }
        val prefs = context.getSharedPreferences("cva_settings", Context.MODE_PRIVATE)
        currentRate = prefs.getFloat("speech_rate", 0.9f).coerceIn(0.5f, 1.5f)

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.setSpeechRate(currentRate)
                pendingTask?.invoke()
                pendingTask = null
                mainHandler.post { onReady?.invoke() }
            } else {
                isInitialized = false
            }
        }
    }

    fun setSpeechRate(rate: Float, context: Context? = null) {
        currentRate = rate.coerceIn(0.5f, 1.5f)
        tts?.setSpeechRate(currentRate)
        context?.getSharedPreferences("cva_settings", Context.MODE_PRIVATE)
            ?.edit()
            ?.putFloat("speech_rate", currentRate)
            ?.apply()
    }

    fun getSpeechRate(): Float = currentRate

    fun speak(
        text: String,
        languageTag: String,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (tts == null || !isInitialized) {
            pendingTask = { speak(text, languageTag, onStart, onDone, onError) }
            return
        }

        val engine = tts ?: return
        val locale = Locale.forLanguageTag(languageTag)
        val avail = engine.isLanguageAvailable(locale)
        if (avail >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = locale
        } else {
            engine.language = Locale.ENGLISH
        }
        engine.setSpeechRate(currentRate)

        val utteranceId = "cybridge_voice_${System.currentTimeMillis()}"
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                mainHandler.post { onStart?.invoke() }
            }

            override fun onDone(id: String?) {
                mainHandler.post { onDone?.invoke() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                mainHandler.post { onError?.invoke("Speech synthesis failed") }
            }

            override fun onError(id: String?, errorCode: Int) {
                mainHandler.post { onError?.invoke("Speech synthesis failed ($errorCode)") }
            }
        })

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Throwable) {}
    }
}
