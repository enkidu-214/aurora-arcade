package com.aurora.arcade.core.games
import org.junit.Assert.*
import org.junit.Test
class Twenty48EngineTest {
    private fun fixture(vararg row:Int):Twenty48Engine = Twenty48Engine.restore("2048v1|7|0|${(row.toList()+List(16-row.size){0}).joinToString(",")}|-")
    @Test fun mergesEachTileOnceAndAddsExactlyOneTile() {
        val e=fixture(2,2,2,2)
        assertTrue(e.move(Twenty48Direction.LEFT))
        assertEquals(listOf(4,4),e.state.cells.take(2)); assertEquals(8,e.state.score)
        assertEquals(3,e.state.cells.count { it>0 })
    }
    @Test fun noOpDoesNotSpawnOrConsumeUndo() {
        val e=fixture(2); val before=e.save()
        assertFalse(e.move(Twenty48Direction.LEFT)); assertEquals(before,e.save()); assertFalse(e.canUndo)
    }
    @Test fun undoRestoresBoardScoreAndRandomState() {
        val e=fixture(2,2); val before=e.save(); e.move(Twenty48Direction.LEFT); val after=e.save()
        assertTrue(e.undo()); assertEquals(before,e.save()); e.move(Twenty48Direction.LEFT); assertEquals(after,e.save())
    }
    @Test fun rightAndDownMergeTowardsDestination() {
        val e=fixture(2,2); e.move(Twenty48Direction.RIGHT); assertEquals(4,e.state.cells[3])
        val d=fixture(2,0,0,0,2); d.move(Twenty48Direction.DOWN); assertEquals(4,d.state.cells[12])
    }
    @Test fun detectsStuckBoardAndRestoresSavedUndo() {
        val e=fixture(2,4,2,4,4,2,4,2,2,4,2,4,4,2,4,2); assertTrue(e.state.over)
        val u=fixture(2,2);u.move(Twenty48Direction.LEFT);val r=Twenty48Engine.restore(u.save()); assertEquals(u.state.cells,r.state.cells);assertTrue(r.undo())
    }
    @Test fun oddSeedsCanSpawnFourTiles() {
        assertTrue((1..199 step 2).any { seed -> Twenty48Engine(seed.toLong()).state.cells.any { it==4 } })
    }
    @Test fun hugeMergeScoreSaturatesInsteadOfOverflowing() {
        val e=fixture(536870912,536870912,536870912,536870912)
        assertTrue(e.move(Twenty48Direction.LEFT));assertEquals(Int.MAX_VALUE,e.state.score)
    }
    @Test fun corruptSaveStartsFresh() { val e=Twenty48Engine.restore("2048v1|0|-1|2,3|-");assertEquals(2,e.state.cells.count{it>0});assertEquals(0,e.state.score) }
}
