package org.koreader.launcher

import android.app.Activity
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.HashMap

class TTSEngine(private val activity: Activity) {
    private val tag = this::class.java.simpleName

    private var tts: TextToSpeech? = null
    private var ttsInitialized = false

    fun ttsInit(): Boolean {
        if (tts != null) {
            return true
        }

        activity.runOnUiThread {
            ttsShutdownInternal()
            tts = TextToSpeech(activity) { status ->
                ttsInitialized = status == TextToSpeech.SUCCESS
                if (!ttsInitialized) {
                    ttsShutdownInternal()
                }
            }
        }

        return true
    }

    fun ttsSpeak(text: String, queueMode: Int): Boolean {
        if (tts == null || !ttsInitialized) {
            ttsInit()
            return false
        }

        return withReadyTts {
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "utteranceId"
            it.speak(text, queueMode, params)
        }
    }

    fun ttsIsSpeaking(): Boolean = tts?.isSpeaking == true

    fun onDestroy() {
        ttsShutdownInternal()
    }

    private fun ttsShutdownInternal() {
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

    fun withReadyTts(action: (TextToSpeech) -> Unit): Boolean {
        val ttsInstance = tts
        if (!ttsInitialized || ttsInstance == null) {
            return false
        }

        activity.runOnUiThread {
            try {
                action(ttsInstance)
            } catch (e: Exception) {
                Log.e(tag, "TTS operation failed", e)
            }
        }

        return true
    }

    fun startActivitySafe(action: String, errorMessage: String) {
        activity.runOnUiThread {
            try {
                activity.startActivity(Intent(action))
            } catch (e: Exception) {
                Log.e(tag, errorMessage, e)
            }
        }
    }
}
