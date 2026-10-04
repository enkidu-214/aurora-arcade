package com.aurora.arcade

import com.aurora.arcade.core.games.PinballEvent
import com.aurora.arcade.core.games.PinballEventKind

internal enum class PinballSound(val volume: Float, val durationMs: Long, val priority: Int = 1) {
    FLIPPER(.36f, 75), LAUNCH(.55f, 240, 2), BUMPER(.48f, 120), SLING(.40f, 95), TARGET(.36f, 85),
    RAMP(.48f, 330, 2), SKILL(.61f, 420, 3), COMBO(.54f, 260, 3), MISSION(.67f, 620, 3),
    MULTIBALL(.68f, 760, 3), JACKPOT(.75f, 700, 3), SAVE(.53f, 320, 3), DRAIN(.43f, 400, 2),
    NUDGE(.26f, 90), GAME_OVER(.48f, 780, 3)
}

/** Six physical voices: contacts may replace contacts, but cannot cut off a reward. */
internal class PinballVoices(size: Int = 6) {
    private val priorities = IntArray(size)
    private val starts = LongArray(size)
    private val ends = LongArray(size)

    fun claim(priority: Int, nowMs: Long, durationMs: Long): Int? {
        var choice = -1
        for (slot in priorities.indices) {
            if (ends[slot] <= nowMs) { choice = slot; break }
            if (priorities[slot] > priority) continue
            if (choice < 0 || priorities[slot] < priorities[choice] ||
                (priorities[slot] == priorities[choice] && starts[slot] < starts[choice])) choice = slot
        }
        if (choice < 0) return null
        priorities[choice] = priority
        starts[choice] = nowMs
        ends[choice] = nowMs + durationMs
        return choice
    }

    fun clear() { priorities.fill(0); starts.fill(0); ends.fill(0) }
}

internal enum class PinballHaptic { TICK, CLICK, REWARD }

internal interface PinballSoundOutput {
    fun play(sound: PinballSound, volume: Float, rate: Float, priority: Int)
    fun stop()
    fun close()
}

/** Selection/throttling has no Android dependency. All timing follows active game time. */
internal class PinballAudio(private val output: PinballSoundOutput) {
    private var closed = false
    private var soundEnabled = true
    private var lastEventTime = -1L
    private val lastSounds = LongArray(PinballSound.entries.size) { -1L }
    private var lastCollisionSound = -1L
    private var lastCollisionHaptic = -1L
    private var lastClickHaptic = -1L
    private var lastRewardHaptic = -1L

    fun play(event: PinballEvent, sound: Boolean, haptic: Boolean): PinballHaptic? {
        if (closed) return null
        val time = event.timeMs.coerceAtLeast(0)
        // A restored/new game can restart the clock. Never carry its predecessor's cooldowns.
        if (time < lastEventTime) resetWindows()
        lastEventTime = time
        if (!sound && soundEnabled) {
            output.stop()
            lastSounds.fill(-1L)
            lastCollisionSound = -1L
        }
        soundEnabled = sound
        val cue = when (event.kind) {
            PinballEventKind.FLIPPER -> PinballSound.FLIPPER
            PinballEventKind.LAUNCH -> PinballSound.LAUNCH
            PinballEventKind.BUMPER -> PinballSound.BUMPER
            PinballEventKind.SLING -> PinballSound.SLING
            PinballEventKind.TARGET -> PinballSound.TARGET
            PinballEventKind.RAMP -> PinballSound.RAMP
            PinballEventKind.SKILL -> PinballSound.SKILL
            PinballEventKind.COMBO -> PinballSound.COMBO
            PinballEventKind.MISSION -> PinballSound.MISSION
            PinballEventKind.MULTIBALL -> PinballSound.MULTIBALL
            PinballEventKind.JACKPOT -> PinballSound.JACKPOT
            PinballEventKind.SAVE -> PinballSound.SAVE
            PinballEventKind.DRAIN -> PinballSound.DRAIN
            PinballEventKind.NUDGE -> PinballSound.NUDGE
            PinballEventKind.GAME_OVER -> PinballSound.GAME_OVER
        }
        val collision = cue == PinballSound.BUMPER || cue == PinballSound.SLING || cue == PinballSound.TARGET
        val action = cue == PinballSound.FLIPPER || cue == PinballSound.NUDGE
        val ready = when {
            collision -> elapsed(time, lastCollisionSound, 20) && elapsed(time, lastSounds[cue.ordinal], 55)
            action -> elapsed(time, lastSounds[cue.ordinal], 35)
            else -> true // Reward events are never swallowed by contact spam.
        }
        if (sound && ready) {
            val rate = if (cue == PinballSound.COMBO)
                1f + event.points.coerceIn(0, 5000) / 25000f else 1f
            output.play(cue, cue.volume, rate, cue.priority)
            lastSounds[cue.ordinal] = time
            if (collision) lastCollisionSound = time
        }
        if (!haptic) return null
        return when {
            collision && elapsed(time, lastCollisionHaptic, 80) -> {
                lastCollisionHaptic = time; PinballHaptic.TICK
            }
            (action || cue == PinballSound.LAUNCH) && elapsed(time, lastClickHaptic, 45) -> {
                lastClickHaptic = time; PinballHaptic.CLICK
            }
            !collision && !action && cue != PinballSound.LAUNCH && elapsed(time, lastRewardHaptic, 70) -> {
                lastRewardHaptic = time; PinballHaptic.REWARD
            }
            else -> null
        }
    }

    fun stop() {
        if (closed) return
        output.stop()
        resetWindows()
    }

    fun close() {
        if (closed) return
        stop()
        closed = true
        output.close()
    }

    private fun resetWindows() {
        lastEventTime = -1L
        lastSounds.fill(-1L)
        lastCollisionSound = -1L
        lastCollisionHaptic = -1L
        lastClickHaptic = -1L
        lastRewardHaptic = -1L
    }

    private fun elapsed(now: Long, previous: Long, gap: Long) = previous < 0 || now - previous >= gap
}
