package com.aurora.arcade

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

/** Original PCM clips are decoded once by SoundPool, never by the simulation/render loop. */
internal class PinballAudioPlayer(context: Context) : PinballSoundOutput {
    private val pool = runCatching {
        SoundPool.Builder().setMaxStreams(6).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()
    }.getOrNull()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()
    private val samples = IntArray(PinballSound.entries.size)
    private val streams = IntArray(6)
    private val voices = PinballVoices(streams.size)
    @Volatile private var closed = false

    init {
        pool?.setOnLoadCompleteListener { _, sample, status ->
            if (!closed && status == 0) loaded.add(sample)
        }
        val resources = mapOf(
            PinballSound.FLIPPER to R.raw.pin_flipper, PinballSound.LAUNCH to R.raw.pin_launch,
            PinballSound.BUMPER to R.raw.pin_bumper, PinballSound.SLING to R.raw.pin_sling,
            PinballSound.TARGET to R.raw.pin_target, PinballSound.RAMP to R.raw.pin_ramp,
            PinballSound.SKILL to R.raw.pin_skill, PinballSound.COMBO to R.raw.pin_combo,
            PinballSound.MISSION to R.raw.pin_mission, PinballSound.MULTIBALL to R.raw.pin_multiball,
            PinballSound.JACKPOT to R.raw.pin_jackpot, PinballSound.SAVE to R.raw.pin_save,
            PinballSound.DRAIN to R.raw.pin_drain, PinballSound.NUDGE to R.raw.pin_nudge,
            PinballSound.GAME_OVER to R.raw.pin_game_over,
        )
        resources.forEach { (sound, resource) ->
            samples[sound.ordinal] = runCatching { pool?.load(context, resource, 1) ?: 0 }.getOrDefault(0)
        }
    }

    override fun play(sound: PinballSound, volume: Float, rate: Float, priority: Int) {
        val player = pool ?: return
        val sample = samples[sound.ordinal]
        // Missed startup sounds stay missed; never replay stale impacts when decoding finishes.
        if (closed || sample !in loaded) return
        val slot = voices.claim(priority, SystemClock.uptimeMillis(), (sound.durationMs / rate).toLong() + 20) ?: return
        if (streams[slot] != 0) player.stop(streams[slot])
        streams[slot] = player.play(sample, volume, volume, priority, 0, rate)
    }

    override fun stop() {
        if (closed) return
        streams.forEach { if (it != 0) pool?.stop(it) }
        streams.fill(0)
        voices.clear()
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
