package org.koreader.launcher

import android.app.Activity
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.HashMap

class TTSEngine(private val activity: Activity) {
    private val tag = this::class.java.simpleName

    @Volatile
    private var tts: TextToSpeech? = null
    @Volatile
    private var ttsInitialized = false
    private val ttsLock = Object()

    fun ttsInit(): Boolean {
        synchronized(ttsLock) {
            if (tts != null) {
                return true
            }
        }

        runOnUiThreadSafe("TTS init") {
            ttsShutdownInternal()
            tts = TextToSpeech(activity) { status ->
                val success = status == TextToSpeech.SUCCESS
                synchronized(ttsLock) {
                    ttsInitialized = success
                }
                if (!success) {
                    ttsShutdownInternal()
                }
            }
        }

        return true
    }

    fun ttsSpeak(text: String, queueMode: Int): Boolean {
        if (!ttsInitialized || tts == null) {
            ttsInit()
            return false
        }

        runOnUiThreadSafe("TTS speak") {
            ttsSpeakInternal(tts!!, text, queueMode)
        }
        return true
    }

    fun ttsStop(): Boolean {
        return withReadyTtsOnUiThread("TTS stop") {
            it.stop()
        }
    }

    fun ttsIsSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    fun ttsSetSpeechRate(ratePercent: Int): Boolean {
        val rate = (ratePercent / 100.0f).coerceIn(0.1f, 4.0f)
        return withReadyTtsOnUiThread("TTS setSpeechRate") {
            it.setSpeechRate(rate)
        }
    }

    fun ttsSetPitch(pitchPercent: Int): Boolean {
        val pitch = (pitchPercent / 100.0f).coerceIn(0.1f, 4.0f)
        return withReadyTtsOnUiThread("TTS setPitch") {
            it.setPitch(pitch)
        }
    }

    fun ttsOpenSettings() {
        runOnUiThreadSafe("TTS openSettings") {
            val intent = Intent()
            // There is no public Settings action constant for TTS settings across all API levels.
            // This is the widely used Settings action string.
            intent.action = "com.android.settings.TTS_SETTINGS"
            try {
                activity.startActivity(intent)
            } catch (e: Exception) {
                Log.e(tag, "Failed to open TTS settings", e)
            }
        }
    }

    fun ttsInstallData() {
        runOnUiThreadSafe("TTS installData") {
            val intent = Intent()
            intent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            try {
                activity.startActivity(intent)
            } catch (e: Exception) {
                Log.e(tag, "Failed to install TTS data", e)
            }
        }
    }

    fun onStop() {
         // Optionally handle lifecycle here if needed, but MainActivity calls ttsShutdownInternal in onDestroy
    }

    fun onDestroy() {
        ttsShutdownInternal()
    }

    private fun ttsShutdownInternal() {
        synchronized(ttsLock) {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                Log.e(tag, "TTS shutdown failed", e)
            } finally {
                tts = null
                ttsInitialized = false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun ttsSpeakInternal(ttsInstance: TextToSpeech, text: String, queueMode: Int): Int {
        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "utteranceId"
        return ttsInstance.speak(text, queueMode, params)
    }

    private fun runOnUiThreadSafe(what: String, action: () -> Unit) {
        activity.runOnUiThread {
            try {
                action()
            } catch (e: Exception) {
                Log.e(tag, "$what failed", e)
            }
        }
    }

    private inline fun withReadyTtsOnUiThread(what: String, crossinline action: (TextToSpeech) -> Unit): Boolean {
        val ttsInstance = tts
        if (!ttsInitialized || ttsInstance == null) {
            return false
        }
        runOnUiThreadSafe(what) {
            action(ttsInstance)
        }
        return true
    }
}
