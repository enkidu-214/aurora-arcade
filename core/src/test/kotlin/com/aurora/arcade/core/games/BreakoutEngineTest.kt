package com.aurora.arcade.core.games
import org.junit.Assert.*
import org.junit.Test
class BreakoutEngineTest {
 private fun fixture(x:Double,y:Double,vx:Double,vy:Double,lives:Int=3,bricks:String=List(40){1}.joinToString(","))=BreakoutEngine.restore("BR1|1|0|$lives|1|500|$x|$y|$vx|$vy|1|0|$bricks")!!
 @Test fun wallAndPaddleReflectWithoutTunneling() {
  val wall=fixture(985.0,600.0,450.0,-50.0); wall.advance(50); assertTrue(wall.state.vx<0)
  val paddle=fixture(500.0,1240.0,0.0,800.0); paddle.advance(50); assertTrue(paddle.state.vy<0); assertEquals(3,paddle.state.lives)
 }
 @Test fun missingPaddleConsumesLifeAndRequiresNewLaunch() {
  val e=fixture(100.0,1395.0,0.0,500.0,1); e.advance(50); assertTrue(e.state.over); assertEquals(0,e.state.lives)
 }
 @Test fun lastBrickAdvancesStage() {
  val e=fixture(106.0,170.0,0.0,-600.0,bricks=List(40){if(it==0)1 else 0}.joinToString(",")); e.advance(50)
  assertEquals(2,e.state.stage); assertFalse(e.state.launched); assertTrue(e.state.score>0)
 }
 @Test fun boundedTimeAndValidatedRestore() {
  val e=BreakoutEngine(1); e.launch(); e.advance(Long.MAX_VALUE); assertTrue(e.state.y>1100)
  assertEquals(e.state,BreakoutEngine.restore(e.save())!!.state); assertNull(BreakoutEngine.restore(e.save().replace("500.0","NaN")))
 }
}
