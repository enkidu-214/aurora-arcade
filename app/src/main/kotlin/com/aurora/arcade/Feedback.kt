package com.aurora.arcade

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View
import com.aurora.arcade.core.EventType

class Feedback(private val view: View) {
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC,28) }.getOrNull()
    fun play(type: EventType?,settings: Settings) {
        if (settings.haptic) view.performHapticFeedback(
            if (type == EventType.CLEAR || type == EventType.DROP) HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.CLOCK_TICK)
        if (settings.sound && type != null) tone?.startTone(when(type) {
            EventType.CLEAR -> ToneGenerator.TONE_PROP_ACK
            EventType.UNDO -> ToneGenerator.TONE_PROP_PROMPT
            EventType.DROP,EventType.LOCK -> ToneGenerator.TONE_PROP_BEEP
            else -> ToneGenerator.TONE_PROP_BEEP2
        },if(type == EventType.CLEAR) 100 else 35)
    }
    fun close() { tone?.release() }
}
