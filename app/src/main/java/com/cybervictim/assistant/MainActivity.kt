package com.cybervictim.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.cybervictim.assistant.ui.AppScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.cybervictim.assistant.ui.L10n.init(this)
        com.cybervictim.assistant.exposure.ExposureApi.init(this)
        com.cybervictim.assistant.data.CaseSession.dbRef =
            java.util.concurrent.atomic.AtomicReference(
                com.cybervictim.assistant.data.CaseDb.get(this)
            )
        setContent {
            com.cybervictim.assistant.ai.CaseSessionStore.current =
                com.cybervictim.assistant.ai.CaseSessionStore()
            val nav = rememberNavController()

            AppScaffold(nav)
        }
    }
}
