package com.aurora.arcade.core

import kotlin.math.pow

/** Deterministic rules independent of Android, the frame rate and visual effects. */
class GameEngine(seed: Long = 1, val mode: Mode = Mode.TIME) {
    var state = GameState(); private set
    var event: GameEvent? = null; private set
    private var eventId = 0L
    private var randomState = seed.takeUnless { it == 0L } ?: 1L
    private val queue = mutableListOf<Kind>()
    private var fallMs = 0L
    private var lockMs = 0L
    private var lockResets = 0
    private var dropPoints = 0
    private var undoSnapshot: EngineSnapshot? = null
    val committedScore get() = undoSnapshot?.state?.score ?: state.score
    val canUndo get() = mode == Mode.TIME && undoSnapshot != null
    val gravityMs get() = (800 * 0.8.pow((state.level - 1).toDouble())).toLong().coerceAtLeast(55)
    val grounded get() = !fits(state.active.copy(y = state.active.y + 1))
    init { spawn(draw(), false) }

    private fun random(bound: Int): Int {
        var x = randomState
        x = x xor (x shl 13); x = x xor (x ushr 7); x = x xor (x shl 17)
        randomState = x
        return ((x ushr 1) % bound).toInt()
    }
    private fun draw(): Kind {
        if (queue.size < 7) {
            val bag = Kind.entries.toMutableList()
            for (i in 6 downTo 1) { val j = random(i + 1); val tmp = bag[i]; bag[i] = bag[j]; bag[j] = tmp }
            queue.addAll(bag)
        }
        return queue.removeAt(0)
    }
    fun fits(piece: Piece): Boolean = Pieces.cells(piece).all {
        it.x in 0 until COLUMNS && it.y in 0 until ROWS && state.board[it.y * COLUMNS + it.x] == 0
    }
    fun landing(piece: Piece = state.active): Piece {
        var result = piece
        while (fits(result.copy(y = result.y + 1))) result = result.copy(y = result.y + 1)
        return result
    }
    private fun spawn(kind: Kind, held: Boolean) {
        val piece = Piece(kind)
        state = state.copy(active = piece, next = queue.toList(), holdUsed = held,
            gameOver = !fits(piece), pieceId = state.pieceId + 1)
        fallMs = 0; lockMs = 0; lockResets = 0; dropPoints = 0
    }
    private fun resetLock(wasGrounded: Boolean) {
        if (wasGrounded && lockResets < 15) { lockMs = 0; lockResets++ }
    }
    fun move(direction: Int): Boolean {
        if (state.gameOver || (direction != -1 && direction != 1)) return false
        val next = state.active.copy(x = state.active.x + direction)
        if (!fits(next)) return false
        val wasGrounded = grounded
        state = state.copy(active = next)
        resetLock(wasGrounded)
        return true
    }
    fun rotate(direction: Int): Boolean {
        if (state.gameOver || (direction != -1 && direction != 1)) return false
        val old = state.active
        val rotation = (old.rotation + direction + 4) % 4
        val target = Pieces.kicks(old.kind, old.rotation, rotation).firstNotNullOfOrNull { kick ->
            old.copy(x = old.x + kick.x, y = old.y + kick.y, rotation = rotation).takeIf { fits(it) }
        } ?: return false
        val wasGrounded = grounded
        state = state.copy(active = target)
        resetLock(wasGrounded)
        event = GameEvent(++eventId, EventType.ROTATE, old, target)
        return true
    }
    fun softDrop(): Boolean {
        if (state.gameOver) return false
        val next = state.active.copy(y = state.active.y + 1)
        if (!fits(next)) return false
        state = state.copy(active = next)
        dropPoints++; fallMs = 0
        return true
    }
    fun hold(): Boolean {
        if (state.gameOver || state.holdUsed) return false
        val old = state.active
        val replacement = state.held ?: draw()
        state = state.copy(held = old.kind)
        spawn(replacement, true)
        event = GameEvent(++eventId, EventType.HOLD, old, state.active)
        return true
    }
    fun hardDrop() {
        if (state.gameOver) return
        val from = state.active
        val target = landing()
        dropPoints += (target.y - from.y) * 2
        state = state.copy(active = target)
        lock(true, from)
    }
    private fun lock(hard: Boolean, from: Piece = state.active) {
        undoSnapshot = if (hard && mode == Mode.TIME) snapshot().copy(
            state = state.copy(active = Piece(state.active.kind)), fallMs = 0, lockMs = 0, lockResets = 0, dropPoints = 0,
            recallFrom = state.active,
        ) else null
        val piece = state.active
        val locked = state.board.toMutableList()
        Pieces.cells(piece).forEach { locked[it.y * COLUMNS + it.x] = piece.kind.ordinal + 1 }
        val rows = (0 until ROWS).filter { y -> (0 until COLUMNS).all { locked[y * COLUMNS + it] != 0 } }
        val combo = if (rows.isEmpty()) -1 else state.combo + 1
        val award = listOf(0,100,300,500,800)[rows.size.coerceAtMost(4)] * state.level +
            (if (combo > 0) 50 * combo * state.level else 0) + dropPoints
        val board = MutableList(rows.size * COLUMNS) { 0 }
        for (y in 0 until ROWS) if (y !in rows) board.addAll(locked.subList(y * COLUMNS, (y + 1) * COLUMNS))
        state = state.copy(board = board, score = state.score + award, lines = state.lines + rows.size, combo = combo)
        spawn(draw(), false)
        if (board.take(HIDDEN_ROWS * COLUMNS).any { it != 0 }) state = state.copy(gameOver = true)
        event = GameEvent(++eventId, if (rows.isNotEmpty()) EventType.CLEAR else if (hard) EventType.DROP else EventType.LOCK,
            from, piece, rows, locked, award)
    }
    fun advance(ms: Long) {
        if (state.gameOver) return
        repeat(ms.coerceIn(0,60_000).toInt()) {
            if (state.gameOver) return
            if (grounded) {
                lockMs++
                if (lockMs >= 500) lock(false)
            } else {
                fallMs++
                if (fallMs >= gravityMs) { fallMs = 0; state = state.copy(active = state.active.copy(y = state.active.y + 1)) }
            }
        }
    }
    fun undo(): Boolean {
        val saved = undoSnapshot?.takeIf { mode == Mode.TIME } ?: return false
        val count = state.undoCount + 1
        val serial = state.pieceId + 1
        restoreSnapshot(saved)
        state = state.copy(undoCount = count, pieceId = serial, gameOver = false)
        undoSnapshot = null
        event = GameEvent(++eventId, EventType.UNDO, saved.recallFrom ?: landing(), state.active)
        return true
    }
    private fun snapshot() = EngineSnapshot(state, randomState, fallMs, lockMs, lockResets, dropPoints)
    fun exportSave() = EngineSave(mode, snapshot(), undoSnapshot)
    private fun restoreSnapshot(saved: EngineSnapshot) {
        state = saved.state.copy(board = saved.state.board.toList(), next = saved.state.next.toList())
        queue.clear(); queue.addAll(state.next)
        randomState = saved.randomState
        fallMs = saved.fallMs; lockMs = saved.lockMs; lockResets = saved.lockResets; dropPoints = saved.dropPoints
    }
    companion object {
        fun restore(save: EngineSave): GameEngine = GameEngine(1, save.mode).apply {
            restoreSnapshot(save.current); undoSnapshot = save.undo
        }
    }
}
