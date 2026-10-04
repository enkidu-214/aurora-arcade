package com.aurora.arcade.core
import org.junit.Assert.*
import org.junit.Test
class RotationAndLockTest {
    private fun at(piece: Piece): GameEngine {
        val save = GameEngine(5).exportSave()
        return GameEngine.restore(save.copy(current = save.current.copy(state = save.current.state.copy(active = piece))))
    }
    @Test fun `T rotates away from left wall`() {
        val game = at(Piece(Kind.T,-1,10,1))
        assertTrue(game.rotate(1))
        assertEquals(0,game.state.active.x)
        assertEquals(2,game.state.active.rotation)
    }
    @Test fun `I wall kick uses two cell offset`() {
        val game = at(Piece(Kind.I,-2,10,1))
        assertTrue(game.rotate(1))
        assertEquals(0,game.state.active.x)
        assertTrue(Pieces.cells(game.state.active).all { it.x in 0..9 })
    }
    @Test fun `grounded piece gets exactly 500ms before lock`() {
        val game = at(Piece(Kind.O,3,22))
        game.advance(499)
        assertEquals(0, game.state.board.count { it != 0 })
        game.advance(1)
        assertEquals(4, game.state.board.count { it != 0 })
    }
    @Test fun `grounded movement cannot reset lock forever`() {
        val game = at(Piece(Kind.O,3,22))
        repeat(15) { game.advance(400); game.move(if(it%2==0) -1 else 1) }
        game.advance(400)
        game.move(1)
        game.advance(100)
        assertEquals(4, game.state.board.count { it != 0 })
    }
    @Test fun `four rotations preserve each shape`() {
        Kind.entries.forEach { kind ->
            val game = at(Piece(kind,3,10))
            repeat(4) { assertTrue(game.rotate(1)) }
            assertEquals(Piece(kind,3,10), game.state.active)
        }
    }
}
