package com.aurora.arcade.core

const val COLUMNS = 10
const val ROWS = 24
const val HIDDEN_ROWS = 4
enum class Mode { TIME, CLASSIC }
enum class Kind { I, O, T, S, Z, J, L }
data class Piece(val kind: Kind, val x: Int = 3, val y: Int = HIDDEN_ROWS, val rotation: Int = 0)
data class GameState(
    val board: List<Int> = List(COLUMNS * ROWS) { 0 },
    val active: Piece = Piece(Kind.T), val next: List<Kind> = emptyList(),
    val held: Kind? = null, val holdUsed: Boolean = false,
    val score: Int = 0, val lines: Int = 0, val combo: Int = -1,
    val gameOver: Boolean = false, val undoCount: Int = 0, val pieceId: Long = 0,
) { val level get() = lines / 10 + 1 }
data class EngineSnapshot(val state: GameState, val randomState: Long,
    val fallMs: Long = 0, val lockMs: Long = 0, val lockResets: Int = 0, val dropPoints: Int = 0,
    val recallFrom: Piece? = null)
data class EngineSave(val mode: Mode, val current: EngineSnapshot, val undo: EngineSnapshot? = null)
enum class EventType { DROP, LOCK, CLEAR, UNDO, HOLD, ROTATE }
data class GameEvent(val id: Long, val type: EventType, val from: Piece, val to: Piece,
    val rows: List<Int> = emptyList(), val lockedBoard: List<Int> = emptyList(), val points: Int = 0)
