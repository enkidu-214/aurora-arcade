package com.aurora.arcade

import com.aurora.arcade.core.games.PinballEvent
import com.aurora.arcade.core.games.PinballEventKind
import org.junit.Assert.*
import org.junit.Test

class PinballAudioTest {
    private val output = RecordingOutput()
    private val audio = PinballAudio(output)

    @Test fun `mechanics and rewards have distinct sound identities`() {
        PinballEventKind.entries.forEachIndexed { index, kind ->
            audio.play(event(kind, index * 200L), true, false)
        }
        assertEquals(15, output.played.size)
        assertEquals(15, output.played.map { it.sound }.distinct().size)
        val bumper = output.played.first { it.sound == PinballSound.BUMPER }
        val jackpot = output.played.first { it.sound == PinballSound.JACKPOT }
        assertTrue(jackpot.priority > bumper.priority)
        assertTrue(jackpot.volume > bumper.volume)
        assertTrue(output.played.all { it.rate in .9f..1.35f && it.volume in 0f..1f })
    }

    @Test fun `multiball collision burst is bounded but every reward reaches output`() {
        repeat(80) { index ->
            val kind = listOf(PinballEventKind.BUMPER, PinballEventKind.SLING, PinballEventKind.TARGET)[index % 3]
            audio.play(event(kind, index.toLong()), true, false)
        }
        assertTrue(output.played.size in 1..6)
        val rewards = listOf(PinballEventKind.SKILL, PinballEventKind.COMBO, PinballEventKind.MISSION,
            PinballEventKind.MULTIBALL, PinballEventKind.JACKPOT, PinballEventKind.SAVE)
        rewards.forEach { audio.play(event(it, 80), true, false) }
        assertEquals(listOf(PinballSound.SKILL, PinballSound.COMBO, PinballSound.MISSION,
            PinballSound.MULTIBALL, PinballSound.JACKPOT, PinballSound.SAVE), output.played.takeLast(6).map { it.sound })
    }

    @Test fun `collision cooldown does not swallow flipper input`() {
        audio.play(event(PinballEventKind.BUMPER, 100), true, false)
        audio.play(event(PinballEventKind.SLING, 100), true, false)
        audio.play(event(PinballEventKind.FLIPPER, 100), true, false)
        assertEquals(listOf(PinballSound.BUMPER, PinballSound.FLIPPER), output.played.map { it.sound })
        audio.play(event(PinballEventKind.BUMPER, 155), true, false)
        assertEquals(PinballSound.BUMPER, output.played.last().sound)
        assertEquals(3, output.played.size)
    }

    @Test fun `sound and haptics switches work independently`() {
        assertEquals(PinballHaptic.CLICK, audio.play(event(PinballEventKind.FLIPPER, 0), false, true))
        assertTrue(output.played.isEmpty())
        assertNull(audio.play(event(PinballEventKind.BUMPER, 100), true, false))
        assertEquals(listOf(PinballSound.BUMPER), output.active)
        assertNull(audio.play(event(PinballEventKind.JACKPOT, 200), false, false))
        assertTrue(output.active.isEmpty())
    }

    @Test fun `haptic collisions are restrained but rewards can punctuate them`() {
        assertEquals(PinballHaptic.TICK, audio.play(event(PinballEventKind.BUMPER, 0), true, true))
        assertNull(audio.play(event(PinballEventKind.TARGET, 20), true, true))
        assertEquals(PinballHaptic.REWARD, audio.play(event(PinballEventKind.JACKPOT, 20), true, true))
        assertNull(audio.play(event(PinballEventKind.COMBO, 21), true, true))
        assertEquals(PinballHaptic.TICK, audio.play(event(PinballEventKind.BUMPER, 100), true, true))
    }

    @Test fun `pause stops all tails and clears throttles for resumed or restarted game`() {
        audio.play(event(PinballEventKind.BUMPER, 1000), true, true)
        audio.stop()
        assertTrue(output.active.isEmpty())
        assertEquals(PinballHaptic.TICK, audio.play(event(PinballEventKind.BUMPER, 0), true, true))
        assertEquals(listOf(PinballSound.BUMPER), output.active)
    }

    @Test fun `restore with backwards event time starts a fresh feedback window`() {
        audio.play(event(PinballEventKind.BUMPER, 1000), true, true)
        assertEquals(PinballHaptic.TICK, audio.play(event(PinballEventKind.BUMPER, 0), true, true))
        assertEquals(2, output.played.size)
    }

    @Test fun `closing is idempotent and late events emit neither sound nor haptics`() {
        audio.play(event(PinballEventKind.MULTIBALL, 0), true, true)
        audio.close()
        audio.close()
        assertNull(audio.play(event(PinballEventKind.JACKPOT, 100), true, true))
        assertEquals(1, output.releases)
        assertTrue(output.active.isEmpty())
        assertEquals(1, output.played.size)
    }

    @Test fun `combo pitch scales gently and caps for large score events`() {
        for (points in listOf(0, 1000, 100000, Int.MAX_VALUE)) {
            audio.play(event(PinballEventKind.COMBO, 0, points), true, false)
        }
        val rates = output.played.map { it.rate }
        assertTrue(rates[1] > rates[0])
        assertTrue(rates.all { it in 1f..1.25f })
        assertEquals(rates[2], rates[3])
    }

    @Test fun `voice allocator lets rewards replace contacts and protects reward tails`() {
        val voices = PinballVoices(3)
        assertEquals(listOf(0, 1, 2), (0..2).map { voices.claim(1, it.toLong(), 100) })
        assertEquals(0, voices.claim(3, 3, 100))
        assertEquals(1, voices.claim(3, 4, 100))
        assertEquals(2, voices.claim(3, 5, 100))
        assertNull(voices.claim(1, 6, 100))
        assertNull(voices.claim(2, 6, 100))
        assertEquals(0, voices.claim(3, 6, 100))
    }

    @Test fun `expired and stopped voices become available to quiet mechanics`() {
        val voices = PinballVoices(1)
        assertEquals(0, voices.claim(3, 0, 100))
        assertNull(voices.claim(1, 99, 50))
        assertEquals(0, voices.claim(1, 100, 50))
        voices.claim(3, 110, 100)
        voices.clear()
        assertEquals(0, voices.claim(1, 111, 50))
    }

    private fun event(kind: PinballEventKind, time: Long, points: Int = 0) =
        PinballEvent(time + 1, kind, 500.0, 700.0, time, points)

    private data class Playback(val sound: PinballSound, val volume: Float, val rate: Float, val priority: Int)
    private class RecordingOutput : PinballSoundOutput {
        val played = mutableListOf<Playback>()
        val active = mutableListOf<PinballSound>()
        var releases = 0
        override fun play(sound: PinballSound, volume: Float, rate: Float, priority: Int) {
            played += Playback(sound, volume, rate, priority)
            active += sound
        }
        override fun stop() { active.clear() }
        override fun close() { releases++ }
    }
}
