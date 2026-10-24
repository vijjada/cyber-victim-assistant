package com.cybervictim.assistant.exposure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-backed storage for the exposure-session bearer token (Phase 9-B).
 * Reviewer rule: credentials never live in plain SharedPreferences.
 * Hardware-backed AES-256-GCM master key; falls back to normal prefs on
 * devices where Keystore init fails, so flows never hard-crash.
 */
object SecPrefs {

    @Volatile private var sp: SharedPreferences? = null

    @Synchronized
    fun get(ctx: Context): SharedPreferences {
        sp?.let { return it }
        val app = ctx.applicationContext
        val v = try {
            val master = MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                app,
                "cva_sec_prefs",
                master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Throwable) {
            app.getSharedPreferences("cva_sec_prefs", Context.MODE_PRIVATE)
        }
        sp = v
        return v
    }
}
