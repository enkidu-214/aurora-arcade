package com.aurora.arcade.core.games
import kotlin.math.*

data class BubblePoint(val x:Double,val y:Double)
data class BubbleShot(val x:Double,val y:Double,val vx:Double,val vy:Double,val color:Int)
data class BubbleState(val board:List<Int>,val current:Int,val next:Int,val level:Int=1,val score:Int=0,val misses:Int=0,val projectile:BubbleShot?=null,val over:Boolean=false,val won:Boolean=false)
/** Staggered hex cells; only bubbles connected to the ceiling remain after a match. */
class BubbleEngine(seed:Long=System.nanoTime(),level:Int=1) {
 companion object {
  const val ROWS=14; const val COLS=9; const val RADIUS=50.0; const val LAUNCH_Y=1300.0; const val ROW_HEIGHT=86.6025403784
  fun valid(row:Int,col:Int)=row in 0 until ROWS && col in 0 until (if(row%2==0)9 else 8)
  fun center(row:Int,col:Int)=BubblePoint(100.0+col*100+if(row%2==1)50 else 0,60.0+row*ROW_HEIGHT)
  fun neighbors(row:Int,col:Int):List<Pair<Int,Int>> {
   val shift=if(row%2==0)-1 else 0
   return listOf(row to col-1,row to col+1,row-1 to col+shift,row-1 to col+shift+1,row+1 to col+shift,row+1 to col+shift+1).filter{valid(it.first,it.second)}
  }
  fun restore(raw:String?):BubbleEngine?=runCatching {
   require(raw!=null && raw.length<3000);val p=raw.split('|');require(p.size==15 && p[0]=="BU1")
   val level=p[2].toInt(); val e=BubbleEngine(p[1].toLong(),level); val current=p[5].toInt(); val next=p[6].toInt();val active=p[7].toInt(); val x=p[8].toDouble();val y=p[9].toDouble();val vx=p[10].toDouble();val vy=p[11].toDouble();val flags=p[12].toInt();val shotColor=p[13].toInt(); val board=p[14].split(',').map(String::toInt)
   require(level in 1..999 && current in 0..4 && next in 0..4 && shotColor in 0..4 && active in 0..1 && flags in 0..2 && p[3].toInt() in 0..100000000 && p[4].toInt() in 0..7)
   require(board.size==ROWS*COLS && board.all{it in -1..4} && board.indices.all{valid(it/COLS,it%COLS) || board[it]==-1})
   require(listOf(x,y,vx,vy).all{it.isFinite()} && x in 49.0..951.0 && y in 0.0..LAUNCH_Y && abs(vx)<=901 && vy in -901.0..0.0)
   require(e.flood(board,(0 until COLS).filter{board[it]>=0}){board[it]>=0}.size==board.count{it>=0}); require(p[3].toInt()%10==0)
   require(active==0 || (flags==0 && hypot(vx,vy) in 890.0..910.0)); require((flags==2)==board.none{it>=0}); require(flags!=0 || board.indices.none{board[it]>=0 && center(it/COLS,it%COLS).y>1160})
   e.rng=p[1].toLong(); e.state=BubbleState(board,current,next,level,p[3].toInt(),p[4].toInt(),if(active==1)BubbleShot(x,y,vx,vy,shotColor) else null,flags>0,flags==2);e
  }.getOrNull()
 }
 private var rng=seed
 private fun random(bound:Int):Int {rng=rng*6364136223846793005L+1442695040888963407L;return ((rng ushr 1)%bound).toInt()}
 private fun initial(level:Int):List<Int> = List(ROWS*COLS){i->if(i/COLS< (4+(level-1).coerceAtMost(4)) && valid(i/COLS,i%COLS))random((3+(level-1)/2).coerceAtMost(5)) else -1}
 var state:BubbleState=run { val board=initial(level.coerceIn(1,999));BubbleState(board,random(3),random(3),level.coerceIn(1,999)) }; private set
 fun launch(angle:Double):Boolean {
  if(state.over || state.projectile!=null || !angle.isFinite())return false
  val a=angle.coerceIn(-PI+.20,-.20);state=state.copy(projectile=BubbleShot(500.0,LAUNCH_Y,cos(a)*900,sin(a)*900,state.current));return true
 }
 fun advance(milliseconds:Long):Boolean {
  if(state.projectile==null || state.over)return false
  val seconds=milliseconds.coerceIn(0,50)/1000.0;val steps=ceil(seconds/.003).toInt().coerceAtLeast(1);repeat(steps){if(state.projectile!=null)step(seconds/steps)};return true
 }
 private fun step(dt:Double) {
  val old=state.projectile?:return;var x=old.x+old.vx*dt;var vx=old.vx;val y=old.y+old.vy*dt
  if(x<RADIUS){x=RADIUS+(RADIUS-x);vx=abs(vx)};if(x>1000-RADIUS){x=1000-RADIUS-(x-(1000-RADIUS));vx=-abs(vx)}
  val shot=old.copy(x=x,y=y,vx=vx)
  val hit=state.board.indices.firstOrNull{state.board[it]>=0 && distance(center(it/COLS,it%COLS),BubblePoint(x,y))<=99.5}
  if(hit!=null || y<=60) { attach(shot,hit);return };state=state.copy(projectile=shot)
 }
 private fun distance(a:BubblePoint,b:BubblePoint)=hypot(a.x-b.x,a.y-b.y)
 private fun attach(shot:BubbleShot,hit:Int?) {
  val candidates=if(hit==null)(0 until COLS).map{0 to it} else neighbors(hit/COLS,hit%COLS)
  val cell=candidates.filter{state.board[it.first*COLS+it.second]<0}.minByOrNull{distance(center(it.first,it.second),BubblePoint(shot.x,shot.y))}
  if(cell==null) {state=state.copy(projectile=null,over=true);return}
  val board=state.board.toMutableList();val at=cell.first*COLS+cell.second;board[at]=shot.color
  val group=flood(board,listOf(at)){board[it]==shot.color};var removed=0;var misses=state.misses
  if(group.size>=3) {
   group.forEach{board[it]=-1};removed=group.size
   val anchored=flood(board,(0 until COLS).filter{board[it]>=0}){board[it]>=0}
   board.indices.filter{board[it]>=0 && it !in anchored}.forEach{board[it]=-1;removed++};misses=0
  } else misses++
  if(misses>=8) {
   for(r in ROWS-1 downTo 2)for(c in 0 until COLS)board[r*COLS+c]=board[(r-2)*COLS+c]
   for(r in 0..1)for(c in 0 until COLS)board[r*COLS+c]=if(valid(r,c))random((3+(state.level-1)/2).coerceAtMost(5)) else -1
   misses=0
  }
  val won=board.none{it>=0};val over=won || board.indices.any{board[it]>=0 && center(it/COLS,it%COLS).y>1160}
  val colors=board.filter{it>=0}.distinct();val current=if(state.next in colors || colors.isEmpty())state.next else colors[random(colors.size)];val next=if(colors.isEmpty())0 else colors[random(colors.size)]
  state=state.copy(board=board,current=current,next=next,score=state.score+removed*10,misses=misses,projectile=null,over=over,won=won)
 }
 private fun flood(board:List<Int>,starts:List<Int>,accept:(Int)->Boolean):Set<Int> {
  val seen=mutableSetOf<Int>();val queue=ArrayDeque<Int>();starts.filter{accept(it)}.forEach{seen.add(it);queue.add(it)}
  while(queue.isNotEmpty()) {val at=queue.removeFirst();neighbors(at/COLS,at%COLS).map{it.first*COLS+it.second}.filter{it !in seen && accept(it)}.forEach{seen.add(it);queue.add(it)}};return seen
 }
 /** The same collision radius and wall reflections as the shot simulation. */
 fun trajectory(angle:Double):List<BubblePoint> {
  if(!angle.isFinite())return emptyList();val a=angle.coerceIn(-PI+.20,-.20);var x=500.0;var y=LAUNCH_Y;var vx=cos(a)*12;val vy=sin(a)*12;val result=mutableListOf(BubblePoint(x,y))
  val occupied=state.board.indices.filter{state.board[it]>=0}.map{center(it/COLS,it%COLS)}
  repeat(600) {x+=vx;y+=vy;if(x<50){x=100-x;vx=abs(vx);result.add(BubblePoint(x,y))};if(x>950){x=1900-x;vx=-abs(vx);result.add(BubblePoint(x,y))}
   if(y<=60 || occupied.any{distance(it,BubblePoint(x,y))<=100}) {result.add(BubblePoint(x,y));return result}
  };result.add(BubblePoint(x,y));return result
 }
 fun save():String {val shot=state.projectile;return listOf("BU1",rng,state.level,state.score,state.misses,state.current,state.next,if(shot!=null)1 else 0,shot?.x?:500.0,shot?.y?:LAUNCH_Y,shot?.vx?:0.0,shot?.vy?:0.0,if(state.won)2 else if(state.over)1 else 0,shot?.color?:state.current,state.board.joinToString(",")).joinToString("|")}
}
