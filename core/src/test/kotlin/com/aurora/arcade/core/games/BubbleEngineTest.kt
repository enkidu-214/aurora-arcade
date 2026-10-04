package com.aurora.arcade.core.games
import org.junit.Assert.*
import org.junit.Test
class BubbleEngineTest {
 @Test fun hexNeighborsRespectOddRowAndEdges() {
  assertEquals(setOf(0 to 0,0 to 1,1 to 1,2 to 0,2 to 1),BubbleEngine.neighbors(1,0).toSet())
  assertFalse(BubbleEngine.neighbors(0,0).contains(1 to -1))
 }
 private fun board(cells:Map<Pair<Int,Int>,Int>):String=List(126){val r=it/9;val c=it%9;cells[r to c]?:-1}.joinToString(",")
 @Test fun threeMatchingBubblesPopAndDetachedIslandFalls() {
  val b=board(mapOf((0 to 4) to 0,(1 to 3) to 0,(2 to 3) to 1))
  val e=BubbleEngine.restore("BU1|1|1|0|0|0|0|1|550|310|0|-900|0|0|$b")!!
  repeat(30){e.advance(16)}; assertTrue(e.state.won); assertTrue(e.state.score>=40)
 }
 @Test fun shallowBankPreviewReachesTheActualCollisionSurface() {
  val e=BubbleEngine(1); val path=e.trajectory(-.2)
  assertTrue(path.last().y<460); assertTrue(path.all{it.x in 49.0..951.0})
 }
 @Test fun bankPreviewReflectsAndShotCannotBeDuplicated() {
  val e=BubbleEngine(1); val path=e.trajectory(-2.6); assertTrue(path.size>=3)
  assertTrue(e.launch(-2.6)); assertFalse(e.launch(-1.5)); e.advance(Long.MAX_VALUE); assertTrue(e.state.projectile!!.y>1150)
 }
 @Test fun unmatchedShotSticksWithoutClearingAndBottomAttachmentLoses() {
  val b=board(mapOf((0 to 4) to 1))
  val e=BubbleEngine.restore("BU1|1|1|0|0|0|0|1|550|310|0|-900|0|0|$b")!!
  repeat(30){e.advance(16)}; assertEquals(2,e.state.board.count{it>=0}); assertEquals(1,e.state.misses); assertFalse(e.state.over)
  val danger=board((0..12).associate{(it to 4) to 1})
  val loss=BubbleEngine.restore("BU1|1|1|0|0|0|0|1|500|1290|0|-900|0|0|$danger")!!
  repeat(20){loss.advance(16)};assertTrue(loss.state.over);assertFalse(loss.state.won);assertNotNull(BubbleEngine.restore(loss.save()))
 }
 @Test fun eightMissesAddRowsAndWonGamesRejectLaunch() {
  val b=board(mapOf((0 to 4) to 1))
  val e=BubbleEngine.restore("BU1|1|1|0|7|0|0|1|550|310|0|-900|0|0|$b")!!
  repeat(30){e.advance(16)};assertEquals(0,e.state.misses);assertTrue(e.state.board.count{it>=0}>=19)
  val win=BubbleEngine.restore("BU1|1|1|40|0|0|0|0|500|1300|0|0|2|0|${board(emptyMap())}")!!
  assertFalse(win.launch(-1.5));assertFalse(win.advance(50))
 }
 @Test fun floatingOrIncompatibleSavedBoardsAreRejected() {
  val floating=board(mapOf((5 to 4) to 0))
  assertNull(BubbleEngine.restore("BU1|1|1|0|0|0|0|0|500|1300|0|0|0|0|$floating"))
 }
 @Test fun saveRoundTripAndMalformedBoardRejected() {
  val e=BubbleEngine(8); e.launch(-1.5); e.advance(16); assertEquals(e.state,BubbleEngine.restore(e.save())!!.state)
  assertNull(BubbleEngine.restore(e.save().replace("BU1","BU9"))); assertNull(BubbleEngine.restore("BU1|bad"))
 }
}
