package com.cybervictim.assistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.cybervictim.assistant.ui.AppColors
import com.cybervictim.assistant.ui.AppScaffold
import com.cybervictim.assistant.ui.AppTheme

object SuspiciousEmailBridge {
    var sharedText: String? = null
    var sharedUri: Uri? = null
    var authCodeReceived: ((code: String, state: String) -> Unit)? = null

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND) {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            @Suppress("DEPRECATION")
            sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } else if (intent.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data != null && data.scheme == "com.cybervictim.assistant" && data.path?.contains("oauth2redirect") == true) {
                val code = data.getQueryParameter("code")
                val state = data.getQueryParameter("state")
                if (code != null && state != null) {
                    authCodeReceived?.invoke(code, state)
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        if (BuildConfig.DEBUG) {
            android.webkit.WebView.setWebContentsDebuggingEnabled(true)
        }
        SuspiciousEmailBridge.handleIntent(intent)
        com.cybervictim.assistant.ui.L10n.init(this)
        com.cybervictim.assistant.voice.TtsManager.init(this)
        com.cybervictim.assistant.exposure.ExposureApi.init(this)
        com.cybervictim.assistant.data.CaseSession.dbRef =
            java.util.concurrent.atomic.AtomicReference(
                com.cybervictim.assistant.data.CaseDb.get(this)
            )
        com.cybervictim.assistant.ai.KbLoader.assets = { path ->
            assets.open(path).use { it.bufferedReader().readText() }
        }
        setContent {
            com.cybervictim.assistant.ai.CaseSessionStore.current =
                com.cybervictim.assistant.ai.CaseSessionStore()
            val nav = rememberNavController()

            AppTheme {
                AppScaffold(nav)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SuspiciousEmailBridge.handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.cybervictim.assistant.voice.TtsManager.shutdown()
    }
}
