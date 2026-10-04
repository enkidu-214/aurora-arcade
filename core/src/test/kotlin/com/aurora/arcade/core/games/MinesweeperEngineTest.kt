package com.aurora.arcade.core.games
import org.junit.Assert.*
import org.junit.Test
class MinesweeperEngineTest {
    @Test fun firstOpeningAndItsNeighborsAreSafe() {
        repeat(20) { seed -> val e=MinesweeperEngine(seed=seed.toLong()+1);e.open(27)
            assertFalse(e.state.lost);assertEquals(10,e.state.mines.count{it});
            for(y in 2..4) for(x in 2..4) assertFalse(e.state.mines[y*8+x])
            assertTrue(e.state.opened.count{it}>1)
        }
    }
    @Test fun flagsBlockOpeningAndCanBeRemoved() {
        val e=MinesweeperEngine();e.flag(0);assertFalse(e.open(0));assertFalse(e.state.generated);e.flag(0);assertTrue(e.open(0))
    }
    @Test fun allSafeCellsWinAndCompletedBoardCannotMutate() {
        val e=MinesweeperEngine(seed=19);e.open(0); e.state.mines.forEachIndexed { i,m -> if(!m)e.open(i) }
        assertTrue(e.state.won);val before=e.save();assertFalse(e.flag(4));assertEquals(before,e.save())
    }
    @Test fun openingMineLosesAndSaveRoundTrips() {
        val e=MinesweeperEngine(seed=9);e.open(0);e.flag(63); val saved=e.save();val r=MinesweeperEngine.restore(saved); assertEquals(saved,r.save())
        r.open(r.state.mines.indexOfFirst{it});assertTrue(r.state.lost)
    }
    @Test fun matchingIncorrectFlagsCanLoseWhenChording() {
        val mineIndices=setOf(1,7,15,23,31,39,47,55,62,63)
        val mines=(0..63).joinToString(""){if(it in mineIndices)"1" else "0"}
        val opened=(0..63).joinToString(""){if(it==0)"1" else "0"}
        val e=MinesweeperEngine.restore("minesv1|EASY|1|1|-1|$mines|$opened|${"0".repeat(64)}")
        assertEquals(1,e.state.adjacent(0));e.flag(8);assertTrue(e.open(0));assertTrue(e.state.lost)
    }
    @Test fun rejectsSaveThatHasAnOpenedMineWithoutLoss() {
        val e=MinesweeperEngine(seed=3);e.open(0);val p=e.save().split('|').toMutableList()
        p[6]=p[6].toCharArray().apply {this[e.state.mines.indexOfFirst{it}]='1'}.concatToString()
        assertFalse(MinesweeperEngine.restore(p.joinToString("|")).state.generated)
    }
    @Test fun mediumAndInvalidSaveHaveValidBoard() {
        val e=MinesweeperEngine(MineDifficulty.MEDIUM);e.open(60);assertEquals(120,e.state.mines.size);assertEquals(20,e.state.mines.count{it})
        assertFalse(MinesweeperEngine.restore("minesv1|garbage").state.generated)
    }
}
