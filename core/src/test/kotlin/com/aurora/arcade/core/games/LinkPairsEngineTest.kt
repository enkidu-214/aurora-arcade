package com.aurora.arcade.core.games

import org.junit.Assert.*
import org.junit.Test

class LinkPairsEngineTest {
    @Test fun onlyMatchesSameKindAndDistinctCells() {
        val e = board(2,2,1,2,1,2)
        assertNull(e.route(0,1)); assertNull(e.route(0,0))
        assertNotNull(e.match(0,2)); assertEquals(2,e.state.remaining)
    }
    @Test fun routesAroundOutsideEdgeWithTwoTurns() {
        val e = board(3,2,1,2,1,2,3,3)
        val path = e.route(0,2)!!
        assertEquals(4,path.size)
        assertTrue(path.any { it.y == -1 })
        assertNotNull(e.match(0,2))
    }
    @Test fun rejectsTrappedPairsRequiringMoreThanTwoTurns() {
        val e = board(4,4,1,2,3,4,5,1,6,7,8,5,6,7,8,2,3,4)
        assertNull(e.route(0,5))
    }
    @Test fun hintAlwaysGivesLegalPairAndWinCannotMutate() {
        val e = board(2,2,1,1,2,2)
        val h = e.hint()!!
        assertNotNull(e.match(h.first,h.second))
        val last = e.hint()!!
        assertNotNull(e.match(last.first,last.second))
        assertTrue(e.state.won)
        assertFalse(e.reshuffle())
        assertNull(e.match(0,1))
    }
    @Test fun reshufflePreservesMultisetAndProvidesFullClearSequence() {
        repeat(20) { seed ->
            val e = LinkPairsEngine(seed.toLong())
            val before = e.state.tiles.sorted()
            assertTrue(e.reshuffle())
            assertEquals(before,e.state.tiles.sorted())
            repeat(24) {
                val h = e.hint()!!
                assertNotNull(e.match(h.first,h.second))
            }
            assertTrue(e.state.won)
        }
    }
    @Test fun partialBoardReshuffleStaysPlayableAndRoundTrips() {
        val e = board(4,2,1,1,2,2,3,3,4,4)
        e.match(0,1); e.match(4,5)
        assertTrue(e.reshuffle())
        assertEquals(4,e.state.remaining)
        val restored = LinkPairsEngine.restore(e.save())!!
        assertEquals(e.state,restored.state)
        while(!restored.state.won) { val h = restored.hint()!!; assertNotNull(restored.match(h.first,h.second)) }
    }
    @Test fun corruptSavesAreRejected() {
        listOf("", "LP2|2|2|0|1,1,2,2", "LP1|200|2|0|1,1", "LP1|2|2|0|1,1,1,2", "LP1|2|2|-1|1,1,2,2", "x".repeat(10000)).forEach { assertNull(LinkPairsEngine.restore(it)) }
    }
    private fun board(w: Int,h: Int,vararg tiles: Int) = LinkPairsEngine.restore("LP1|$w|$h|0|${tiles.joinToString(",")}")!!
}
