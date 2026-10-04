package com.aurora.arcade

import com.aurora.arcade.core.*
import org.junit.Assert.*
import org.junit.Test

class GameAudioTest {
    private val output = RecordingOutput()
    private val audio = GameAudio(output)

    @Test fun `clearing more rows has distinct stronger feedback than rotation`() {
        audio.play(event(EventType.ROTATE), true)
        for (rows in 1..4) audio.play(event(EventType.CLEAR, rows), true)
        assertEquals(listOf(GameSound.ROTATE, GameSound.CLEAR_ONE, GameSound.CLEAR_TWO,
            GameSound.CLEAR_THREE, GameSound.CLEAR_FOUR), output.played.map { it.sound })
        assertTrue(output.played.zipWithNext().all { (a, b) -> b.volume > a.volume })
        assertTrue(output.played.drop(1).all { it.priority > output.played.first().priority })
    }

    @Test fun `combo pitch rises then caps and resets with the next chain`() {
        for (combo in listOf(0, 1, 3, 100, 101, 0, -1)) {
            audio.play(event(EventType.CLEAR, 2), true, combo)
        }
        val rates = output.played.map { it.rate }
        assertEquals(7, rates.size)
        assertEquals(1f, rates[0])
        assertTrue(rates[1] > rates[0] && rates[2] > rates[1])
        assertTrue(rates.all { it in 1f..1.35f })
        assertEquals(rates[3], rates[4])
        assertEquals(1f, rates[5])
        assertEquals(1f, rates[6])
    }

    @Test fun `muting cancels an active clear and emits no new sound`() {
        audio.play(event(EventType.CLEAR, 4), true)
        audio.play(event(EventType.ROTATE), false)
        assertTrue(output.active.isEmpty())
        assertEquals(listOf(GameSound.CLEAR_FOUR), output.played.map { it.sound })
    }

    @Test fun `movement stays quiet even when sound is enabled`() {
        repeat(100) { audio.play(null, true) }
        assertTrue(output.played.isEmpty())
    }

    @Test fun `recall cancels the previous celebration before playing its own sound`() {
        audio.play(event(EventType.CLEAR, 4), true)
        audio.play(event(EventType.UNDO), true)
        assertEquals(listOf(GameSound.UNDO), output.active)
    }

    @Test fun `pause stops tails while a later game event can play normally`() {
        audio.play(event(EventType.CLEAR, 4), true)
        audio.stop()
        assertTrue(output.active.isEmpty())
        audio.play(event(EventType.DROP), true)
        assertEquals(listOf(GameSound.DROP), output.active)
    }

    @Test fun `closing releases once and ignores late events`() {
        audio.play(event(EventType.CLEAR, 4), true)
        audio.close()
        audio.close()
        audio.play(event(EventType.DROP), true)
        assertTrue(output.active.isEmpty())
        assertEquals(1, output.releases)
        assertEquals(1, output.played.size)
    }

    private fun event(type: EventType, rows: Int = 0) = GameEvent(
        1, type, Piece(Kind.I), Piece(Kind.I), rows = (0 until rows).toList())

    private data class Playback(val sound: GameSound, val volume: Float, val rate: Float, val priority: Int)

    // Only the Android audio device is replaced; selection and lifecycle policy are real.
    private class RecordingOutput : SoundOutput {
        val played = mutableListOf<Playback>()
        val active = mutableListOf<GameSound>()
        var releases = 0
        override fun play(sound: GameSound, volume: Float, rate: Float, priority: Int) {
            played += Playback(sound, volume, rate, priority)
            active += sound
        }
        override fun stop() { active.clear() }
        override fun close() { releases++ }
    }
}
