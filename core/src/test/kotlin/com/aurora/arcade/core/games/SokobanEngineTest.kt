package com.aurora.arcade.core.games
import org.junit.Assert.*
import org.junit.Test
class SokobanEngineTest {
    @Test fun blockedMoveDoesNotCountAndUndoRestoresPush() {
        val e=SokobanEngine();assertFalse(e.move(SokobanDirection.UP)); val before=e.save()
        assertTrue(e.move(SokobanDirection.RIGHT));assertEquals(1,e.state.pushes);assertTrue(e.undo());assertEquals(before,e.save())
    }
    @Test fun firstLevelCompletesAndCannotMutateUntilNextLevel() {
        val e=SokobanEngine();e.move(SokobanDirection.RIGHT);assertTrue(e.state.complete)
        assertFalse(e.move(SokobanDirection.LEFT)); assertTrue(e.nextLevel());assertEquals(1,e.state.level)
    }
    @Test fun cannotPushBoxThroughWallAndResetClearsUndo() {
        val e=SokobanEngine(1);assertTrue(e.move(SokobanDirection.RIGHT));val before=e.save()
        assertFalse(e.move(SokobanDirection.RIGHT));assertEquals(before,e.save())
        e.reset();assertEquals(0,e.state.moves);assertFalse(e.canUndo)
    }
    @Test fun savesUndoAndRejectsCorruption() {
        val e=SokobanEngine();e.move(SokobanDirection.RIGHT);val r=SokobanEngine.restore(e.save());assertEquals(e.save(),r.save());assertTrue(r.undo())
        assertEquals(0,SokobanEngine.restore("sokov1|999|4|1|2|-").state.level)
    }
    @Test fun everyHandAuthoredLevelHasASolution() {
        repeat(SokobanEngine.levelCount) { level ->
            val start=SokobanEngine(level).state
            data class Node(val player:Int,val boxes:Set<Int>)
            val queue=java.util.ArrayDeque<Node>();val seen=HashSet<Node>();queue.add(Node(start.player,start.boxes));var solved=false
            while(queue.isNotEmpty() && seen.size<150000) {
                val n=queue.removeFirst();if(!seen.add(n))continue
                if(n.boxes==start.goals) { solved=true;break }
                listOf(-start.width,start.width,-1,1).forEach { delta ->
                    val to=n.player+delta; if(to in start.floor && (delta!=1 && delta!=-1 || to/start.width==n.player/start.width)) {
                        if(to !in n.boxes)queue.add(Node(to,n.boxes))
                        else { val beyond=to+delta;if(beyond in start.floor && beyond !in n.boxes && (delta!=1 && delta!=-1 || beyond/start.width==to/start.width))queue.add(Node(to,n.boxes-to+beyond)) }
                    }
                }
            }
            assertTrue("Level ${level+1} must be solvable (${seen.size} states)",solved)
        }
    }
}
