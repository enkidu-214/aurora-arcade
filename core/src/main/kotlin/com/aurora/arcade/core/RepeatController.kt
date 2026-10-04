package com.aurora.arcade.core

enum class Action { LEFT, RIGHT, SOFT, CW, CCW, DROP, HOLD, UNDO }
/** Multi-touch repeat scheduling, driven by the same monotonic clock as the game. */
class RepeatController(val delayMs: Long = 140, val intervalMs: Long = 40) {
    private val held = linkedMapOf<Action,Long>()
    init { require(delayMs > 0 && intervalMs > 0) }
    fun press(action: Action): List<Action> {
        if (action in held) return emptyList()
        held[action] = if (action == Action.SOFT) 35L else delayMs
        return listOf(action)
    }
    fun release(action: Action) {
        held.remove(action)
        if (action == Action.LEFT || action == Action.RIGHT) {
            held.keys.filter { it == Action.LEFT || it == Action.RIGHT }.forEach { held[it] = delayMs }
        }
    }
    fun advance(ms: Long): List<Action> {
        val result = mutableListOf<Action>()
        val horizontal = held.keys.lastOrNull { it == Action.LEFT || it == Action.RIGHT }
        for (action in held.keys.toList()) {
            if (action != Action.SOFT && action != horizontal) continue
            var remaining = held.getValue(action) - ms.coerceIn(0,1000)
            val interval = if (action == Action.SOFT) 35L else intervalMs
            while (remaining <= 0) { result += action; remaining += interval }
            held[action] = remaining
        }
        return result
    }
    fun clear() { held.clear() }
}
