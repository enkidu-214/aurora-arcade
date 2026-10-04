package com.aurora.arcade.core.games

import org.junit.Assert.*
import org.junit.Test

class SpiderEngineTest {
    @Test fun authenticDealHasEightCopiesOfEveryRank() {
        val s = SpiderEngine(3).state
        assertEquals(listOf(6,6,6,6,5,5,5,5,5,5), s.columns.map { it.size })
        assertTrue(s.columns.all { c -> c.last().faceUp && c.dropLast(1).none { it.faceUp } })
        assertEquals(50, s.stock.size)
        for (rank in 1..13) assertEquals(8, s.columns.flatten().count { it.rank == rank } + s.stock.count { it == rank })
    }
    @Test fun movesDescendingSequenceAndRevealsCardAndUndoes() {
        val e = fixture(listOf(listOf(-9,7,6), listOf(8)))
        val before = e.save()
        assertFalse(e.move(0,0,1))
        assertTrue(e.move(0,1,1))
        assertEquals(listOf(8,7,6), e.state.columns[1].map { it.rank })
        assertTrue(e.state.columns[0].last().faceUp)
        assertTrue(e.undo())
        assertEquals(before, e.save())
    }
    @Test fun rejectsBrokenSequencesAndWrongRankButAllowsEmptyColumn() {
        val e = fixture(listOf(listOf(7,5),listOf(8)))
        assertFalse(e.move(0,0,1))
        assertFalse(e.move(0,1,1))
        assertTrue(e.move(0,1,2))
    }
    @Test fun cannotDealWithEmptyColumnAndDealIsUndoable() {
        val empty = fixture(listOf(listOf(7)), stockCount = 10)
        assertFalse(empty.deal())
        val e = SpiderEngine(8)
        val before = e.save()
        assertTrue(e.deal())
        assertEquals(40, e.state.stock.size)
        assertTrue(e.state.columns.all { it.last().faceUp })
        assertTrue(e.undo())
        assertEquals(before,e.save())
    }
    @Test fun completesLastKingToAceSetAndWinsThenUndoRestores() {
        val cols = List(10) { mutableListOf<Int>() }
        cols[0].addAll((13 downTo 2).toList()); cols[1].add(1)
        val e = SpiderEngine.restore("SP1|7|0||" + cols.joinToString(";") { it.joinToString(",") })!!
        assertTrue(e.move(1,0,0))
        assertEquals(8,e.state.completed)
        assertTrue(e.state.won)
        assertTrue(e.state.columns.all { it.isEmpty() })
        assertFalse(e.deal())
        assertTrue(e.undo())
        assertFalse(e.state.won)
        assertEquals(7,e.state.completed)
    }
    @Test fun hintIsLegalAndSavePreservesUndo() {
        val e = fixture(listOf(listOf(7,6),listOf(8)))
        val hint = e.hint()!!
        assertTrue(e.move(hint.from,hint.card,hint.to))
        val restored = SpiderEngine.restore(e.save())!!
        assertEquals(e.state, restored.state)
        assertTrue(restored.undo())
        assertEquals(0,restored.state.moves)
    }
    @Test fun rejectsInvalidOrOversizedSaves() {
        listOf("", "SP2|0|0||", "SP1|8|0|1|;;;;;;;;;", "SP1|0|0||1;;;;;;;;;", "x".repeat(70000)).forEach { assertNull(SpiderEngine.restore(it)) }
        val valid = SpiderEngine(4).save()
        assertNull(SpiderEngine.restore(valid.replaceFirst("SP1|0|0|", "SP1|9|0|")))
    }
    private fun fixture(initial: List<List<Int>>, stockCount: Int = 0): SpiderEngine {
        val counts = IntArray(14) { if(it == 0) 0 else 8 }
        val cols = List(10) { mutableListOf<Int>() }
        initial.forEachIndexed { index, cards -> cards.forEach { cols[index].add(it); counts[kotlin.math.abs(it)]-- } }
        val leftover = (1..13).flatMap { rank -> List(counts[rank]) { rank } }
        val stock = leftover.take(stockCount)
        cols[9].addAll(leftover.drop(stockCount))
        return SpiderEngine.restore("SP1|0|0|${stock.joinToString(",")}|${cols.joinToString(";") { it.joinToString(",") }}")!!
    }
}
