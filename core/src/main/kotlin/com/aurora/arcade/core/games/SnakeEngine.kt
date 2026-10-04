package com.aurora.arcade.core.games

/** One cell per tick; the two-slot input queue prevents rapid taps from reversing the head. */
enum class SnakeDirection(val dx:Int,val dy:Int) { UP(0,-1), RIGHT(1,0), DOWN(0,1), LEFT(-1,0) }
data class SnakeState(val body:List<Int>,val food:Int,val direction:SnakeDirection,val score:Int=0,val fixed:Boolean=false,val over:Boolean=false,val won:Boolean=false)
class SnakeEngine(seed:Long=System.nanoTime()) {
 companion object {
  const val WIDTH=18; const val HEIGHT=24
  fun restore(raw:String?):SnakeEngine? = runCatching {
   require(raw!=null && raw.length<6000); val p=raw.split('|'); require(p.size==10 && p[0]=="SN1")
   val e=SnakeEngine(p[1].toLong()); val score=p[2].toInt(); val fixed=p[3].toInt(); val direction=SnakeDirection.valueOf(p[4]); val over=p[5].toInt(); val won=p[6].toInt(); val elapsed=p[7].toLong(); val food=p[8].toInt(); val body=p[9].split(',').map(String::toInt)
   require(score in 0..100000 && score%10==0 && fixed in 0..1 && over in 0..1 && won in 0..1 && elapsed in 0..219 && body.size in 3..432 && body.distinct().size==body.size && body.all{it in 0..431})
   require(body.zipWithNext().all{(a,b)->kotlin.math.abs(a%WIDTH-b%WIDTH)+kotlin.math.abs(a/WIDTH-b/WIDTH)==1})
   require((food in 0..431 && food !in body && won==0) || (food==-1 && body.size==432 && won==1 && over==1))
   require(score==(body.size-3)*10); require(over==1 || (body[0]%WIDTH-body[1]%WIDTH==direction.dx && body[0]/WIDTH-body[1]/WIDTH==direction.dy)); require(won==0 || over==1); e.rng=p[1].toLong(); e.elapsed=elapsed; e.state=SnakeState(body,food,direction,score,fixed==1,over==1,won==1); e
  }.getOrNull()
 }
 private var rng=seed; private var elapsed=0L; private val turns=ArrayDeque<SnakeDirection>()
 var state=SnakeState(listOf(224,223,222),225,SnakeDirection.RIGHT); private set
 val interval:Long get()=if(state.fixed)220 else (220-state.score*2).coerceAtLeast(75).toLong()
 fun setFixed(fixed:Boolean) { state=state.copy(fixed=fixed); elapsed=0 }
 fun turn(direction:SnakeDirection):Boolean {
  if(state.over || turns.size>=2) return false
  val last=turns.lastOrNull()?:state.direction
  if(last==direction || last.dx==-direction.dx && last.dy==-direction.dy) return false
  turns.addLast(direction); return true
 }
 fun advance(milliseconds:Long):Boolean {
  if(state.over) return false
  elapsed+=milliseconds.coerceIn(0,250); var changed=false
  while(elapsed>=interval && !state.over) { elapsed-=interval; tick(); changed=true }
  return changed
 }
 private fun tick() {
  val direction=if(turns.isEmpty())state.direction else turns.removeFirst(); val head=state.body.first(); val x=head%WIDTH+direction.dx; val y=head/WIDTH+direction.dy; val next=y*WIDTH+x; val eating=next==state.food
  if(x !in 0 until WIDTH || y !in 0 until HEIGHT || next in if(eating)state.body else state.body.dropLast(1)) { state=state.copy(direction=direction,over=true); elapsed=0; turns.clear(); return }
  val body=listOf(next)+if(eating)state.body else state.body.dropLast(1); val won=body.size==WIDTH*HEIGHT
  if(won)elapsed=0
  state=state.copy(body=body,direction=direction,score=state.score+if(eating)10 else 0,food=if(won)-1 else if(eating)newFood(body) else state.food,over=won,won=won)
 }
 private fun newFood(body:List<Int>):Int { val free=(0 until WIDTH*HEIGHT).filter{it !in body}; rng=rng*6364136223846793005L+1442695040888963407L; return free[((rng ushr 1)%free.size).toInt()] }
 fun save():String= listOf("SN1",rng,state.score,if(state.fixed)1 else 0,state.direction.name,if(state.over)1 else 0,if(state.won)1 else 0,elapsed,state.food,state.body.joinToString(",")).joinToString("|")
}
