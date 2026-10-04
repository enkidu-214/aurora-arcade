package com.aurora.arcade

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.HapticFeedbackConstants
import android.view.View
import com.aurora.arcade.core.EventType
import com.aurora.arcade.core.GameEvent
import com.aurora.arcade.core.games.PinballEvent
import java.util.concurrent.ConcurrentHashMap

class Feedback(private val view: View) {
    private val audio = GameAudio(SoundPoolOutput(view.context.applicationContext))
    private val pinballAudio = PinballAudio(PinballAudioPlayer(view.context.applicationContext))

    fun play(event: GameEvent?, settings: Settings, combo: Int = 0) {
        if (settings.haptic) view.performHapticFeedback(
            if (event?.type == EventType.CLEAR || event?.type == EventType.DROP)
                HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.CLOCK_TICK)
        audio.play(event, settings.sound, combo)
    }

    fun pinball(event: PinballEvent, settings: Settings) {
        val haptic = pinballAudio.play(event, settings.sound, settings.haptic) ?: return
        view.performHapticFeedback(when (haptic) {
            PinballHaptic.TICK -> HapticFeedbackConstants.CLOCK_TICK
            PinballHaptic.CLICK -> HapticFeedbackConstants.CONTEXT_CLICK
            PinballHaptic.REWARD -> HapticFeedbackConstants.LONG_PRESS
        })
    }

    fun stop() { audio.stop(); pinballAudio.stop() }
    fun close() { audio.close(); pinballAudio.close() }
}

private class SoundPoolOutput(context: Context) : SoundOutput {
    private val pool = runCatching {
        SoundPool.Builder().setMaxStreams(4).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()
    }.getOrNull()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()
    private val samples = IntArray(GameSound.entries.size)
    private val streams = IntArray(GameSound.entries.size)
    @Volatile private var closed = false

    init {
        pool?.setOnLoadCompleteListener { _, sample, status ->
            if (!closed && status == 0) loaded.add(sample)
        }
        val resources = mapOf(
            GameSound.CLEAR_ONE to R.raw.clear_one, GameSound.CLEAR_TWO to R.raw.clear_two,
            GameSound.CLEAR_THREE to R.raw.clear_three, GameSound.CLEAR_FOUR to R.raw.clear_four,
            GameSound.DROP to R.raw.drop, GameSound.LOCK to R.raw.lock,
            GameSound.ROTATE to R.raw.rotate, GameSound.HOLD to R.raw.hold, GameSound.UNDO to R.raw.undo
        )
        // Decode once on SoundPool's worker, never during input or a game frame.
        resources.forEach { (sound, resource) ->
            samples[sound.ordinal] = runCatching { pool?.load(context, resource, 1) ?: 0 }.getOrDefault(0)
        }
    }

    override fun play(sound: GameSound, volume: Float, rate: Float, priority: Int) {
        val player = pool ?: return
        val sample = samples[sound.ordinal]
        // Do not queue late sounds: a missed startup click is better than delayed feedback.
        if (closed || sample !in loaded) return
        // All clears share one voice, so rapid clears replace the previous tail.
        val slot = if (sound.priority == 2) GameSound.CLEAR_ONE.ordinal else sound.ordinal
        if (streams[slot] != 0) player.stop(streams[slot])
        streams[slot] = player.play(sample, volume, volume, priority, 0, rate)
    }

    override fun stop() {
        if (closed) return
        streams.forEach { if (it != 0) pool?.stop(it) }
        streams.fill(0)
    }

    override fun close() {
        if (closed) return
        stop()
        closed = true
        pool?.setOnLoadCompleteListener(null)
        pool?.release()
        loaded.clear()
    }
}
