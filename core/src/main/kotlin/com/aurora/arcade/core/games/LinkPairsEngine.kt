package com.aurora.arcade.core.games

import kotlin.random.Random

data class LinkPoint(val x: Int,val y: Int)
data class LinkState(val width: Int,val height: Int,val tiles: List<Int>,val moves: Int = 0) {
    val remaining: Int get() = tiles.count { it != 0 }
    val won: Boolean get() = remaining == 0
}

/** Untimed matching with at most two corners, including the empty outside perimeter. */
class LinkPairsEngine(seed: Long = System.nanoTime()) {
    private val random = Random(seed)
    var state: LinkState = LinkState(6,8,List(48) { (it/2)%12+1 }.shuffled(random)); private set
    init { if (hint() == null) reshuffle() }

    fun route(first: Int, second: Int): List<LinkPoint>? {
        val tiles = state.tiles
        if (first !in tiles.indices || second !in tiles.indices || first == second || tiles[first] == 0 || tiles[first] != tiles[second]) return null
        val start = LinkPoint(first % state.width,first / state.width)
        val end = LinkPoint(second % state.width,second / state.width)
        val paddedWidth = state.width + 2
        val best = IntArray(paddedWidth*(state.height+2)*4) { 3 }
        data class Step(val point: LinkPoint,val direction: Int,val turns: Int,val path: List<LinkPoint>)
        val queue = ArrayDeque<Step>()
        val directions = listOf(LinkPoint(1,0),LinkPoint(0,1),LinkPoint(-1,0),LinkPoint(0,-1))
        queue.add(Step(start,-1,0,listOf(start)))
        while(queue.isNotEmpty()) {
            val step = queue.removeFirst()
            for ((direction,delta) in directions.withIndex()) {
                val point = LinkPoint(step.point.x+delta.x,step.point.y+delta.y)
                if(point.x !in -1..state.width || point.y !in -1..state.height) continue
                val turns = step.turns + if(step.direction == -1 || step.direction == direction) 0 else 1
                if(turns > 2) continue
                if(point != end && point.x in 0 until state.width && point.y in 0 until state.height && tiles[point.y*state.width+point.x] != 0) continue
                val index = ((point.y+1)*paddedWidth+point.x+1)*4+direction
                if(turns >= best[index]) continue
                best[index] = turns
                val path = if(step.direction == -1 || direction != step.direction) step.path + point else step.path.dropLast(1) + point
                if(point == end) return path
                queue.add(Step(point,direction,turns,path))
            }
        }
        return null
    }
    fun match(first: Int,second: Int): List<LinkPoint>? {
        val path = route(first,second) ?: return null
        val tiles = state.tiles.toMutableList()
        tiles[first] = 0; tiles[second] = 0
        state = state.copy(tiles = tiles.toList(),moves = state.moves+1)
        // A match may leave a deadlock. Reorder remaining tiles so play always continues.
        if (!state.won && hint() == null) reshuffle()
        return path
    }
    fun hint(): Pair<Int,Int>? {
        for(first in state.tiles.indices) {
            if(state.tiles[first] == 0) continue
            for(second in first+1 until state.tiles.size) {
                if(state.tiles[first] == state.tiles[second] && route(first,second) != null) return first to second
            }
        }
        return null
    }
    fun reshuffle(): Boolean {
        if(state.won) return false
        val pairKinds = state.tiles.filter { it > 0 }.groupingBy { it }.eachCount().flatMap { (kind,count) -> List(count/2) { kind } }.shuffled(random)
        val tiles = MutableList(state.tiles.size) { 0 }
        val pairs = mutableListOf<Pair<Int,Int>>()
        val leftovers = mutableListOf<Int>()
        // Consecutive occupied positions within a row connect directly. Once those
        // pairs clear, row leftovers connect through the outside perimeter.
        for(y in 0 until state.height) {
            val occupied = (0 until state.width).map { y*state.width+it }.filter { state.tiles[it] > 0 }
            occupied.chunked(2).forEach { if(it.size == 2) pairs.add(it[0] to it[1]) else leftovers.add(it[0]) }
        }
        leftovers.chunked(2).forEach { pairs.add(it[0] to it[1]) }
        pairs.forEachIndexed { index,(a,b) -> tiles[a] = pairKinds[index]; tiles[b] = pairKinds[index] }
        state = state.copy(tiles = tiles.toList())
        return true
    }
    fun save(): String = "LP1|${state.width}|${state.height}|${state.moves}|${state.tiles.joinToString(",")}"
    companion object {
        fun restore(value: String): LinkPairsEngine? = runCatching {
            require(value.length in 1..4096)
            val p = value.split('|'); require(p.size == 5 && p[0] == "LP1")
            val width = p[1].toInt(); val height = p[2].toInt(); val moves = p[3].toInt()
            require(width in 2..10 && height in 2..12 && width*height % 2 == 0 && moves in 0..1_000_000)
            val tiles = p[4].split(',').map(String::toInt)
            require(tiles.size == width*height && tiles.all { it in 0..12 })
            require(tiles.filter { it > 0 }.groupingBy { it }.eachCount().values.all { it % 2 == 0 })
            LinkPairsEngine(0).apply { state = LinkState(width,height,tiles,moves) }
        }.getOrNull()
    }
}
