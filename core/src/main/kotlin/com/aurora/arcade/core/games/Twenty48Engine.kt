package com.aurora.arcade.core.games

/** Logical tiles are immutable; a move also supplies source/destination trails for rendering. */
enum class Twenty48Direction { UP, DOWN, LEFT, RIGHT }
data class Twenty48Trail(val from:Int,val to:Int,val value:Int)
data class Twenty48State(val cells:List<Int>,val score:Int,val over:Boolean,val won:Boolean)
class Twenty48Engine(seed:Long=System.nanoTime()) {
    private var random=seed and 0x7fffffffL
    private var cells=MutableList(16){0}
    private var score=0
    private var previous:String?=null
    var trails:List<Twenty48Trail> = emptyList(); private set
    var spawned:Int=-1; private set
    val canUndo get()=previous!=null
    val state get()=Twenty48State(cells.toList(),score,!canMove(),cells.any{it>=2048})
    init { spawn();spawn();trails=emptyList() }
    private fun next(bound:Int):Int { random=(random*1103515245L+12345L) and 0x7fffffffL;return ((random ushr 8)%bound).toInt() }
    private fun spawn() { val empty=cells.indices.filter{cells[it]==0};spawned=if(empty.isEmpty())-1 else empty[next(empty.size)];if(spawned>=0)cells[spawned]=if(next(10)==0)4 else 2 }
    fun move(direction:Twenty48Direction):Boolean {
        val result=MutableList(16){0}; val motion=mutableListOf<Twenty48Trail>();var gain=0L
        repeat(4) { line ->
            val positions=(0..3).map { offset -> when(direction) {
                Twenty48Direction.LEFT->line*4+offset;Twenty48Direction.RIGHT->line*4+3-offset
                Twenty48Direction.UP->offset*4+line;Twenty48Direction.DOWN->(3-offset)*4+line
            } }
            val tiles=positions.filter{cells[it]!=0};var read=0;var write=0
            while(read<tiles.size) {
                val from=tiles[read];val target=positions[write++];var value=cells[from]
                motion+=Twenty48Trail(from,target,value)
                if(read+1<tiles.size && cells[tiles[read+1]]==value && value<1073741824) {
                    motion+=Twenty48Trail(tiles[++read],target,value);value*=2;gain+=value
                }
                result[target]=value;read++
            }
        }
        if(result==cells)return false
        previous=snapshot();cells=result;score=(score.toLong()+gain).coerceAtMost(Int.MAX_VALUE.toLong()).toInt();trails=motion.toList();spawn();return true
    }
    fun undo():Boolean { val raw=previous?:return false;readSnapshot(raw);previous=null;trails=emptyList();spawned=-1;return true }
    private fun canMove():Boolean = cells.any{it==0} || cells.indices.any { i ->
        cells[i]<1073741824 && ((i%4<3 && cells[i]==cells[i+1]) || (i<12 && cells[i]==cells[i+4]))
    }
    private fun snapshot()="$random;$score;${cells.joinToString(",")}"
    private fun readSnapshot(raw:String) {
        val p=raw.split(';');require(p.size==3);val r=p[0].toLong();val s=p[1].toInt();val c=p[2].split(',').map{it.toInt()}
        require(r in 0..0x7fffffffL && s>=0 && c.size==16 && c.all { it==0 || (it>=2 && it<=1073741824 && it and (it-1)==0) } && c.any{it>0})
        random=r;score=s;cells=c.toMutableList()
    }
    fun save()="2048v1|$random|$score|${cells.joinToString(",")}|${previous?:"-"}"
    companion object {
        fun restore(raw:String?):Twenty48Engine = runCatching {
            require(raw!=null && raw.length<2048);val p=raw.split('|');require(p.size==5 && p[0]=="2048v1")
            Twenty48Engine(1).apply {
                readSnapshot("${p[1]};${p[2]};${p[3]}");val current=snapshot()
                if(p[4]!="-") { readSnapshot(p[4]);previous=p[4];readSnapshot(current) }
                trails=emptyList();spawned=-1
            }
        }.getOrElse { Twenty48Engine() }
    }
}
