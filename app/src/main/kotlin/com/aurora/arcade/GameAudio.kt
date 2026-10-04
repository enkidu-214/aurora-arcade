package com.aurora.arcade

import com.aurora.arcade.core.GameEvent
import com.aurora.arcade.core.EventType
import kotlin.math.pow

internal enum class GameSound(val volume: Float, val priority: Int = 1) {
    CLEAR_ONE(.62f, 2), CLEAR_TWO(.69f, 2), CLEAR_THREE(.77f, 2), CLEAR_FOUR(.85f, 2),
    DROP(.48f), LOCK(.28f), ROTATE(.20f), HOLD(.35f), UNDO(.40f)
}

internal interface SoundOutput {
    fun play(sound: GameSound, volume: Float, rate: Float, priority: Int)
    fun stop()
    fun close()
}

internal class GameAudio(private val output: SoundOutput) {
    private var closed = false

    fun play(event: GameEvent?, enabled: Boolean, combo: Int = 0) {
        if (closed) return
        if (!enabled) { stop(); return }
        if (event == null) return
        val sound = when (event.type) {
            EventType.CLEAR -> when (event.rows.size) {
                1 -> GameSound.CLEAR_ONE
                2 -> GameSound.CLEAR_TWO
                3 -> GameSound.CLEAR_THREE
                else -> GameSound.CLEAR_FOUR
            }
            EventType.DROP -> GameSound.DROP
            EventType.LOCK -> GameSound.LOCK
            EventType.ROTATE -> GameSound.ROTATE
            EventType.HOLD -> GameSound.HOLD
            EventType.UNDO -> GameSound.UNDO
        }
        // Recall invalidates the celebration that the previous landing started.
        if (event.type == EventType.UNDO) stop()
        val rate = if (event.type == EventType.CLEAR) 2.0.pow(combo.coerceIn(0, 5) / 12.0).toFloat() else 1f
        output.play(sound, sound.volume, rate, sound.priority)
    }

    fun stop() { if (!closed) output.stop() }

    fun close() {
        if (closed) return
        stop()
        closed = true
        output.close()
    }
}
