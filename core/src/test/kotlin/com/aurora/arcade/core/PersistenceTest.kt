package com.aurora.arcade.core
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
class PersistenceTest {
    @Test fun `replacement store waits behind older store pending saves`() {
        val directory=Files.createTempDirectory("aurora-test").toFile()
        try {
            val file=directory.resolve("session.bin")
            val first=GameFileStore(file)
            val game=GameEngine(49)
            repeat(12) { game.move(if(it%2==0) -1 else 1); first.save(game.exportSave()) }
            val expected=game.exportSave()
            val replacement=GameFileStore(file)
            assertEquals(expected,replacement.load())
            game.hardDrop(); replacement.save(game.exportSave())
            assertEquals(game.exportSave(),first.load())
        } finally { directory.deleteRecursively() }
    }
    @Test fun `missing or damaged save is safe`() {
        val directory=Files.createTempDirectory("aurora-test").toFile()
        try {
            val file=directory.resolve("session.bin")
            val store=GameFileStore(file)
            assertNull(store.load())
            file.writeBytes(byteArrayOf(1,2,3))
            assertNull(store.load())
        } finally { directory.deleteRecursively() }
    }
    @Test fun `records exclude reversible drop points until committed`() {
        val game=GameEngine(23)
        game.hardDrop()
        assertTrue(game.state.score>0)
        assertEquals(0,game.committedScore)
        val restored=GameEngine.restore(game.exportSave())
        assertEquals(0,restored.committedScore)
        assertTrue(restored.undo())
        assertEquals(0,restored.committedScore)
        val firstScore=game.state.score
        game.hardDrop()
        assertEquals(firstScore,game.committedScore)
        val classic=GameEngine(23,Mode.CLASSIC)
        classic.hardDrop()
        assertEquals(classic.state.score,classic.committedScore)
    }
}
