package com.aurora.arcade.core.games
import org.junit.Assert.*
import org.junit.Test
class SnakeEngineTest {
 @Test fun reversalIsRejectedAndTwoTurnsAreBuffered() {
  val e=SnakeEngine(1); assertFalse(e.turn(SnakeDirection.LEFT)); assertTrue(e.turn(SnakeDirection.UP)); assertTrue(e.turn(SnakeDirection.LEFT))
  e.advance(220); assertEquals(SnakeDirection.UP,e.state.direction); e.advance(220); assertEquals(SnakeDirection.LEFT,e.state.direction)
 }
 @Test fun foodGrowsBodyAndWallEndsGame() {
  val e=SnakeEngine.restore("SN1|1|0|0|RIGHT|0|0|0|225|224,223,222")!!
  e.advance(220); assertEquals(10,e.state.score); assertEquals(4,e.state.body.size)
  val wall=SnakeEngine.restore("SN1|1|0|0|RIGHT|0|0|0|50|233,232,231")!!
  wall.advance(220); assertTrue(wall.state.over)
 }
 @Test fun selfCollisionEndsGameButMovingIntoVacatedTailIsLegal() {
  val e=SnakeEngine.restore("SN1|1|70|0|RIGHT|0|0|0|225|95,94,76,77,78,96,114,113,131,132")!!
  e.turn(SnakeDirection.DOWN); e.advance(100); assertTrue(e.state.over)
  val tail=SnakeEngine.restore("SN1|1|50|0|RIGHT|0|0|0|225|95,94,76,77,78,96,114,113")!!
  tail.turn(SnakeDirection.DOWN); tail.advance(120); assertFalse(tail.state.over); assertEquals(113,tail.state.body.first())
 }
 @Test fun terminalSaveSurvivesLargeElapsedTime() {
  val e=SnakeEngine.restore("SN1|1|70|0|RIGHT|0|0|79|225|95,94,76,77,78,96,114,113,131,132")!!
  e.turn(SnakeDirection.DOWN);e.advance(250); assertNotNull(SnakeEngine.restore(e.save()))
 }
 @Test fun fillingLastFreeCellWinsAndRestores() {
  val path=(0 until 24).flatMap{row->(if(row%2==0)(0 until 18).toList() else (17 downTo 0).toList()).map{row*18+it}}
  val body=path.drop(1).joinToString(",")
  val e=SnakeEngine.restore("SN1|1|4280|0|LEFT|0|0|74|0|$body")!!
  e.advance(250);assertTrue(e.state.won);assertEquals(432,e.state.body.size);assertNotNull(SnakeEngine.restore(e.save()))
 }
 @Test fun impossibleScoreAndLiveHeadDirectionAreRejected() {
  assertNull(SnakeEngine.restore("SN1|1|10|0|RIGHT|0|0|0|225|224,223,222"))
  assertNull(SnakeEngine.restore("SN1|1|0|0|LEFT|0|0|0|225|224,223,222"))
 }
 @Test fun elapsedTimeIsBoundedAndCorruptSavesRejected() {
  val e=SnakeEngine(1); e.advance(Long.MAX_VALUE); assertEquals(225,e.state.body.first()); assertNull(SnakeEngine.restore("SN1|1|0|0|RIGHT|0|0|0|225|224,224"))
  assertEquals(e.state,SnakeEngine.restore(e.save())!!.state)
 }
}
