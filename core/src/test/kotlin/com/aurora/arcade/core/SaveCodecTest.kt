package com.aurora.arcade.core
import org.junit.Assert.*
import org.junit.Test
class SaveCodecTest {
    @Test fun `saved game restores future pieces timers score and undo`() {
        val game = GameEngine(182)
        game.hold(); game.softDrop(); game.hardDrop(); game.move(-1); game.advance(173)
        val saved = game.exportSave()
        assertEquals(saved, SaveCodec.decode(SaveCodec.encode(saved)))
        val restored = GameEngine.restore(SaveCodec.decode(SaveCodec.encode(saved))!!)
        assertTrue(restored.undo())
        assertTrue(game.undo())
        assertEquals(game.state,restored.state)
        repeat(5) { game.hardDrop(); restored.hardDrop(); assertEquals(game.state,restored.state) }
    }
    @Test fun `truncated and corrupted data is rejected`() {
        val bytes = SaveCodec.encode(GameEngine(9).exportSave())
        assertTrue(bytes.size > 30)
        assertNull(SaveCodec.decode(bytes.copyOf(20)))
        bytes[0] = 0
        assertNull(SaveCodec.decode(bytes))
        assertNull(SaveCodec.decode(ByteArray(100_000)))
    }
}
