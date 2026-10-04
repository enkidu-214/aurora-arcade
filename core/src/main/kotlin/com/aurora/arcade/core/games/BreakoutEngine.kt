package com.aurora.arcade.core.games
import kotlin.math.*

data class BreakoutState(val paddle:Double=500.0,val x:Double=500.0,val y:Double=1251.0,val vx:Double=170.0,val vy:Double=-440.0,val bricks:List<Int> = List(40){1},val score:Int=0,val lives:Int=3,val stage:Int=1,val launched:Boolean=false,val over:Boolean=false)
class BreakoutEngine(seed:Long=System.nanoTime()) {
 companion object {
  const val WIDTH=1000.0; const val HEIGHT=1400.0; const val RADIUS=14.0; const val PADDLE_Y=1280.0; const val PADDLE_HALF=100.0
  fun brickX(index:Int)=52.0+index%8*112.0
  fun brickY(index:Int)=100.0+index/8*62.0
  fun restore(raw:String?):BreakoutEngine?=runCatching {
   require(raw!=null && raw.length<2000); val p=raw.split('|'); require(p.size==13 && p[0]=="BR1")
   val e=BreakoutEngine(p[1].toLong()); val s=BreakoutState(p[5].toDouble(),p[6].toDouble(),p[7].toDouble(),p[8].toDouble(),p[9].toDouble(),p[12].split(',').map(String::toInt),p[2].toInt(),p[3].toInt(),p[4].toInt(),p[10]=="1",p[11]=="1")
   require(listOf(s.paddle,s.x,s.y,s.vx,s.vy).all{it.isFinite()} && s.paddle in 100.0..900.0 && s.x in 0.0..1000.0 && s.y in 0.0..1430.0 && abs(s.vx)<=1000 && abs(s.vy)<=1000)
   require(s.bricks.size==40 && s.bricks.all{it in 0..2} && s.bricks.any{it>0} && s.score in 0..100000000 && s.lives in 0..3 && s.stage in 1..999 && p[10] in listOf("0","1") && p[11] in listOf("0","1") && s.over==(s.lives==0))
   require(!s.launched || hypot(s.vx,s.vy) in 100.0..1100.0); e.state=s; e
  }.getOrNull()
 }
 private val seed=seed
 var state=BreakoutState(); private set
 fun movePaddle(x:Double) { if(!x.isFinite() || state.over)return; val paddle=x.coerceIn(PADDLE_HALF,WIDTH-PADDLE_HALF); state=state.copy(paddle=paddle,x=if(state.launched)state.x else paddle) }
 fun launch():Boolean { if(state.over || state.launched)return false; state=state.copy(launched=true); return true }
 fun advance(milliseconds:Long):Boolean {
  if(!state.launched || state.over)return false
  val seconds=milliseconds.coerceIn(0,50)/1000.0; val steps=ceil(seconds/.003).toInt().coerceAtLeast(1); repeat(steps){if(state.launched && !state.over)step(seconds/steps)}; return true
 }
 private fun step(dt:Double) {
  val old=state; var x=old.x+old.vx*dt; var y=old.y+old.vy*dt; var vx=old.vx; var vy=old.vy
  if(x<RADIUS) {x=RADIUS;vx=abs(vx)}; if(x>WIDTH-RADIUS){x=WIDTH-RADIUS;vx=-abs(vx)}; if(y<RADIUS){y=RADIUS;vy=abs(vy)}
  if(vy>0 && old.y<=PADDLE_Y-RADIUS && y>=PADDLE_Y-RADIUS && abs(x-old.paddle)<=PADDLE_HALF+RADIUS) {
   val offset=((x-old.paddle)/PADDLE_HALF).coerceIn(-.95,.95); val speed=hypot(vx,vy).coerceIn(460.0,850.0); vx=speed*sin(offset*1.05); vy=-speed*cos(offset*1.05); y=PADDLE_Y-RADIUS
  }
  val bricks=old.bricks.toMutableList(); var score=old.score
  for(i in bricks.indices) {
   if(bricks[i]==0)continue
   val left=brickX(i);val top=brickY(i);val right=left+100;val bottom=top+42
   if(x>=left-RADIUS && x<=right+RADIUS && y>=top-RADIUS && y<=bottom+RADIUS) {
    bricks[i]--;score+=if(bricks[i]==0)10 else 5
    if(old.y<top-RADIUS){y=top-RADIUS;vy=-abs(vy)} else if(old.y>bottom+RADIUS){y=bottom+RADIUS;vy=abs(vy)} else if(old.x<left){x=left-RADIUS;vx=-abs(vx)} else {x=right+RADIUS;vx=abs(vx)}
    break
   }
  }
  if(bricks.none{it>0}) { val stage=(old.stage+1).coerceAtMost(999);state=BreakoutState(paddle=old.paddle,x=old.paddle,bricks=List(40){if(stage>=3 && (it+stage)%4==0)2 else 1},score=score+100,lives=old.lives,stage=stage,vy=-(440+stage*25).coerceAtMost(760).toDouble());return }
  if(y>HEIGHT+RADIUS) { val lives=old.lives-1;state=old.copy(x=old.paddle,y=PADDLE_Y-RADIUS-15,vx=170.0,vy=-(440+old.stage*25).coerceAtMost(760).toDouble(),bricks=bricks,score=score,lives=lives,over=lives==0,launched=false);return }
  state=old.copy(x=x,y=y,vx=vx,vy=vy,bricks=bricks,score=score)
 }
 fun save()=listOf("BR1",seed,state.score,state.lives,state.stage,state.paddle,state.x,state.y,state.vx,state.vy,if(state.launched)1 else 0,if(state.over)1 else 0,state.bricks.joinToString(",")).joinToString("|")
}
