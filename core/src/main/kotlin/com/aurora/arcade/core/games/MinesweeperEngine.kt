package com.aurora.arcade.core.games

enum class MineDifficulty(val width:Int,val height:Int,val count:Int) { EASY(8,8,10),MEDIUM(10,12,20) }
data class MineState(val difficulty:MineDifficulty,val mines:List<Boolean>,val opened:List<Boolean>,val flags:List<Boolean>,val generated:Boolean,val lost:Boolean,val won:Boolean,val exploded:Int) {
    val width get()=difficulty.width
    val height get()=difficulty.height
    val remaining get()=difficulty.count-flags.count{it}
    val cleared get()=opened.count{it}-if(lost)1 else 0
    fun adjacent(index:Int):Int { val x=index%width;val y=index/width;var count=0
        for(dy in -1..1)for(dx in -1..1)if((dx!=0 || dy!=0) && x+dx in 0 until width && y+dy in 0 until height && mines[(y+dy)*width+x+dx])count++
        return count
    }
}
class MinesweeperEngine(val difficulty:MineDifficulty=MineDifficulty.EASY,seed:Long=System.nanoTime()) {
    private var random=seed and 0x7fffffffL
    private val size=difficulty.width*difficulty.height
    private var mines=MutableList(size){false};private var opened=MutableList(size){false};private var flags=MutableList(size){false}
    private var generated=false;private var exploded=-1
    val state get()=MineState(difficulty,mines.toList(),opened.toList(),flags.toList(),generated,exploded>=0,generated && exploded<0 && opened.count{it}==size-difficulty.count,exploded)
    private fun next(bound:Int):Int {random=(random*1103515245L+12345L) and 0x7fffffffL;return ((random ushr 8)%bound).toInt()}
    private fun neighbors(i:Int):List<Int> {val x=i%difficulty.width;val y=i/difficulty.width;return buildList {for(dy in -1..1)for(dx in -1..1)if((dx!=0 || dy!=0) && x+dx in 0 until difficulty.width && y+dy in 0 until difficulty.height)add((y+dy)*difficulty.width+x+dx)}}
    private fun generate(first:Int) {val safe=(neighbors(first)+first).toSet();val available=(0 until size).filter{it !in safe}.toMutableList()
        repeat(difficulty.count) {mines[available.removeAt(next(available.size))]=true};generated=true
    }
    fun flag(i:Int):Boolean {if(i !in 0 until size || opened[i] || state.lost || state.won)return false;flags[i]=!flags[i];return true}
    fun open(i:Int):Boolean {
        if(i !in 0 until size || flags[i] || state.lost || state.won)return false
        if(!generated)generate(i)
        if(opened[i])return chord(i)
        reveal(i);return true
    }
    /** Tapping an exposed number opens its neighbors once the adjacent flag count matches. */
    private fun chord(i:Int):Boolean { val nearby=neighbors(i);if(nearby.count{flags[it]}!=nearby.count{mines[it]})return false
        val targets=nearby.filter{!flags[it] && !opened[it]};if(targets.isEmpty())return false
        for(target in targets) {reveal(target);if(exploded>=0)break};return true
    }
    private fun reveal(i:Int) {
        if(mines[i]) {opened[i]=true;exploded=i;return}
        val queue=java.util.ArrayDeque<Int>();queue.add(i)
        while(queue.isNotEmpty()) {val n=queue.removeFirst();if(opened[n] || flags[n] || mines[n])continue;opened[n]=true
            val nearby=neighbors(n);if(nearby.none{mines[it]})nearby.filter{!opened[it] && !flags[it]}.forEach{queue.add(it)}
        }
    }
    fun save()="minesv1|${difficulty.name}|$random|${if(generated)1 else 0}|$exploded|${bits(mines)}|${bits(opened)}|${bits(flags)}"
    private fun bits(list:List<Boolean>)=list.joinToString(""){if(it)"1" else "0"}
    companion object {
        fun restore(raw:String?):MinesweeperEngine=runCatching {
            require(raw!=null && raw.length<2048);val p=raw.split('|');require(p.size==8 && p[0]=="minesv1")
            MinesweeperEngine(MineDifficulty.valueOf(p[1]),p[2].toLong()).apply {
                require(p[2].toLong() in 0..0x7fffffffL && p[3] in listOf("0","1"));generated=p[3]=="1";exploded=p[4].toInt()
                fun read(s:String):MutableList<Boolean> {require(s.length==size && s.all{it=='0'||it=='1'});return s.map{it=='1'}.toMutableList()}
                mines=read(p[5]);opened=read(p[6]);flags=read(p[7]);require(exploded in -1 until size)
                require(mines.count{it}==if(generated)difficulty.count else 0)
                require(generated || opened.none{it});require(opened.indices.none{opened[it] && flags[it]})
                require(opened.indices.filter{opened[it] && mines[it]}==if(exploded<0)emptyList<Int>() else listOf(exploded))
            }
        }.getOrElse {MinesweeperEngine()}
    }
}
