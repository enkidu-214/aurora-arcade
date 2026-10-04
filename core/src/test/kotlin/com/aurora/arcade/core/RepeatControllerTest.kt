package com.aurora.arcade.core
import org.junit.Assert.*
import org.junit.Test
class RepeatControllerTest {
    @Test fun `press is immediate with delayed repeat and no sticky release`() {
        val input = RepeatController()
        assertEquals(listOf(Action.LEFT), input.press(Action.LEFT))
        assertTrue(input.advance(139).isEmpty())
        assertEquals(listOf(Action.LEFT),input.advance(1))
        assertEquals(listOf(Action.LEFT,Action.LEFT),input.advance(80))
        input.release(Action.LEFT)
        assertTrue(input.advance(1000).isEmpty())
    }
    @Test fun `drop rotation hold and undo never auto repeat`() {
        val input = RepeatController()
        listOf(Action.DROP,Action.CW,Action.CCW,Action.HOLD,Action.UNDO).forEach {
            assertEquals(listOf(it),input.press(it))
            assertTrue(input.press(it).isEmpty())
            assertTrue(input.advance(1000).isEmpty())
            input.release(it)
        }
    }
    @Test fun `latest horizontal direction wins while soft drop combines`() {
        val input = RepeatController()
        input.press(Action.LEFT); input.press(Action.RIGHT); input.press(Action.SOFT)
        val actions = input.advance(140)
        assertFalse(actions.contains(Action.LEFT))
        assertTrue(actions.contains(Action.RIGHT))
        assertTrue(actions.contains(Action.SOFT))
        input.clear()
        assertTrue(input.advance(1000).isEmpty())
    }
}
