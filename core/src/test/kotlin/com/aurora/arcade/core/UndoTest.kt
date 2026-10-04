package com.aurora.arcade.core

import org.junit.Assert.*
import org.junit.Test

class UndoTest {
    @Test fun `undo restores cleared rows score level and combo`() {
        val original = GameEngine(91).exportSave()
        val board = MutableList(240) { 0 }
        for (y in 20..23) for (x in 0..9) if (x != 4) board[y*10+x] = 6
        val state = original.current.state.copy(board = board, active = Piece(Kind.I,2,4,1), score = 500, lines = 9, combo = -1)
        val game = GameEngine.restore(original.copy(current = original.current.copy(state = state)))
        game.hardDrop()
        assertEquals(13, game.state.lines)
        assertEquals(2, game.state.level)
        assertEquals(1332, game.state.score)
        assertTrue(game.undo())
        assertEquals(board, game.state.board)
        assertEquals(500, game.state.score)
        assertEquals(9, game.state.lines)
        assertEquals(-1, game.state.combo)
        assertEquals(Piece(Kind.I), game.state.active)
        assertFalse(game.undo())
    }
    @Test fun `undo restores next sequence and hold even after next piece was held`() {
        val game = GameEngine(778)
        game.hold()
        val original = game.state
        game.hardDrop()
        game.hold()
        game.move(1)
        assertTrue(game.undo())
        assertEquals(original.next, game.state.next)
        assertEquals(original.held, game.state.held)
        assertEquals(original.holdUsed, game.state.holdUsed)
        assertEquals(original.active.kind, game.state.active.kind)
        assertEquals(1, game.state.undoCount)
        val expected = GameEngine.restore(game.exportSave())
        repeat(6) { game.hardDrop(); expected.hardDrop(); assertEquals(expected.state.next, game.state.next) }
    }
    @Test fun `repeat drop and undo cannot farm points`() {
        val game = GameEngine(4)
        repeat(8) {
            game.softDrop(); game.softDrop(); game.hardDrop()
            assertTrue(game.state.score > 0)
            assertTrue(game.undo())
            assertEquals(0, game.state.score)
            assertTrue(game.state.board.all { it == 0 })
        }
        assertEquals(8, game.state.undoCount)
    }
    @Test fun `a fatal hard drop can be undone`() {
        val original = GameEngine(91).exportSave()
        val board = MutableList(240) { 0 }.apply { this[64] = 3; this[65] = 3 }
        val state = original.current.state.copy(board = board, active = Piece(Kind.O))
        val game = GameEngine.restore(original.copy(current = original.current.copy(state = state)))
        game.hardDrop()
        assertTrue(game.state.gameOver)
        assertTrue(game.canUndo)
        assertTrue(game.undo())
        assertFalse(game.state.gameOver)
        assertEquals(board, game.state.board)
    }
    @Test fun `automatic lock expires previous undo and classic never has undo`() {
        val game = GameEngine(3)
        game.hardDrop()
        assertTrue(game.canUndo)
        while (game.softDrop()) { }
        game.advance(500)
        assertFalse(game.canUndo)
        val classic = GameEngine(3,Mode.CLASSIC)
        classic.hardDrop()
        assertFalse(classic.undo())
        assertFalse(classic.canUndo)
    }
    @Test fun `second hard drop can only undo second piece`() {
        val game = GameEngine(83)
        game.hardDrop()
        val board = game.state.board
        val score = game.state.score
        game.hardDrop()
        assertTrue(game.undo())
        assertEquals(board, game.state.board)
        assertEquals(score, game.state.score)
        assertFalse(game.canUndo)
        assertFalse(game.undo())
        assertEquals(board, game.state.board)
    }
}
