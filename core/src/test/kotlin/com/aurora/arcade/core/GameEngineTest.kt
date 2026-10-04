package com.aurora.arcade.core

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    @Test fun `pieces move immediately and stop at walls`() {
        val game = GameEngine(7)
        assertTrue(game.move(-1))
        assertEquals(2, game.state.active.x)
        repeat(20) { game.move(-1) }
        val edge = game.state.active.x
        assertFalse(game.move(-1))
        assertEquals(edge, game.state.active.x)
    }

    @Test fun `hard drop locks four cells and advances queue`() {
        val game = GameEngine(7)
        val next = game.state.next.firstOrNull()
        game.hardDrop()
        assertEquals(4, game.state.board.count { it != 0 })
        assertEquals(next, game.state.active.kind)
        assertTrue(game.state.score > 0)
    }

    @Test fun `each aligned bag contains seven distinct pieces`() {
        val game = GameEngine(72)
        val seen = mutableSetOf<Kind>()
        repeat(7) {
            seen += game.state.active.kind
            game.hardDrop()
        }
        assertEquals(7, seen.size)
    }

    @Test fun `hold allowed only once before lock`() {
        val game = GameEngine(2)
        val first = game.state.active.kind
        assertTrue(game.hold())
        assertEquals(first, game.state.held)
        assertFalse(game.hold())
        game.hardDrop()
        assertTrue(game.hold())
    }

    @Test fun `gravity moves on elapsed time independent of frames`() {
        val a = GameEngine(8)
        val b = GameEngine(8)
        a.advance(800)
        repeat(50) { b.advance(16) }
        assertEquals(5, a.state.active.y)
        assertEquals(a.state, b.state)
    }
}
