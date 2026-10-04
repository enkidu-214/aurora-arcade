package com.aurora.arcade.core.games

enum class SokobanDirection { UP,DOWN,LEFT,RIGHT }
data class SokobanState(val level:Int,val width:Int,val height:Int,val floor:Set<Int>,val goals:Set<Int>,val player:Int,val boxes:Set<Int>,val moves:Int,val pushes:Int,val complete:Boolean)
class SokobanEngine(level:Int=0) {
    private var level=level.coerceIn(0,levelCount-1)
    private var player=0;private var boxes=emptySet<Int>();private var moves=0;private var pushes=0
    private var floor=emptySet<Int>();private var goals=emptySet<Int>();private var width=0;private var height=0
    private val history=java.util.ArrayDeque<String>()
    val canUndo get()=history.isNotEmpty()
    val state get()=SokobanState(level,width,height,floor.toSet(),goals.toSet(),player,boxes.toSet(),moves,pushes,boxes==goals)
    init { setup() }
    private fun setup() {
        val rows=levels[level];width=rows.maxOf{it.length};height=rows.size;val f=mutableSetOf<Int>();val g=mutableSetOf<Int>();val b=mutableSetOf<Int>()
        rows.forEachIndexed { y,row -> row.forEachIndexed { x,c -> val i=y*width+x;if(c!='#')f.add(i);if(c in ".+*")g.add(i);if(c in "$*")b.add(i);if(c in "@+")player=i } }
        floor=f.toSet();goals=g.toSet();boxes=b.toSet();moves=0;pushes=0;history.clear()
    }
    fun reset() {setup()}
    fun move(direction:SokobanDirection):Boolean {
        if(state.complete)return false
        val delta=when(direction){SokobanDirection.UP->-width;SokobanDirection.DOWN->width;SokobanDirection.LEFT->-1;SokobanDirection.RIGHT->1}
        val to=player+delta;if(to !in floor || (delta in listOf(-1,1) && to/width!=player/width))return false
        val pushing=to in boxes;val beyond=to+delta
        if(pushing && (beyond !in floor || beyond in boxes || (delta in listOf(-1,1) && beyond/width!=to/width)))return false
        history.addLast(snapshot());if(history.size>200)history.removeFirst()
        if(pushing) {boxes=boxes-to+beyond;pushes++};player=to;moves++;return true
    }
    fun undo():Boolean {if(history.isEmpty())return false;readSnapshot(history.removeLast());return true}
    fun nextLevel():Boolean {if(!state.complete || level==levelCount-1)return false;level++;setup();return true}
    private fun snapshot()="$player,$moves,$pushes;${boxes.sorted().joinToString(",")}"
    private fun readSnapshot(raw:String) {val p=raw.split(';');require(p.size==2);val a=p[0].split(',').map{it.toInt()};val b=p[1].split(',').map{it.toInt()};require(a.size==3 && a[0] in floor && a[1] in 0..1000000 && a[2] in 0..a[1] && b.size==goals.size && b.toSet().size==b.size && b.all{it in floor} && a[0] !in b);player=a[0];moves=a[1];pushes=a[2];boxes=b.toSet()}
    fun save()="sokov1|$level|${snapshot()}|${history.joinToString("/")}"
    companion object {
        private val levels=listOf(
            listOf("#####","#@$.#","#####"),
            listOf("######","#    #","# @$ #","#  . #","######"),
            listOf("#######","#     #","# .$. #","#  $  #","#  @  #","#######"),
            listOf("#######","#  .  #","#  $  #","# . $ #","#  @  #","#     #","#######"),
            listOf("########","#      #","# .##  #","#  $   #","#   $. #","# @    #","########"),
            listOf("########","# . .  #","# $ $  #","#  #   #","#  @   #","#      #","########"),
            listOf("########","#  .   #","#  $   #","# .#.$ #","#  $   #","#   @  #","#      #","########"),
            listOf("########","# . .  #","#      #","# $$#  #","# . $  #","#  @   #","#      #","########")
        )
        val levelCount get()=levels.size
        fun restore(raw:String?):SokobanEngine=runCatching {
            require(raw!=null && raw.length<30000);val p=raw.split('|');require(p.size==4 && p[0]=="sokov1");val level=p[1].toInt();require(level in levels.indices)
            SokobanEngine(level).apply {
                val old=if(p[3].isEmpty())emptyList() else p[3].split('/');require(old.size<=200)
                old.forEach{readSnapshot(it);history.addLast(it)};readSnapshot(p[2])
            }
        }.getOrElse{SokobanEngine()}
    }
}
