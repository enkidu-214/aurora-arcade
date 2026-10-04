package com.aurora.arcade.core.games

import kotlin.random.Random

data class SpiderCard(val rank: Int, val faceUp: Boolean)
data class SpiderMove(val from: Int, val card: Int, val to: Int)
data class SpiderState(val columns: List<List<SpiderCard>>, val stock: List<Int>, val completed: Int = 0, val moves: Int = 0) {
    val won: Boolean get() = completed == 8
}

/** Authentic single-suit Spider. Each successful action is one undo transaction. */
class SpiderEngine(seed: Long = System.nanoTime()) {
    var state: SpiderState = initial(seed); private set
    private val history = ArrayDeque<SpiderState>()
    val canUndo: Boolean get() = history.isNotEmpty()
    val canDeal: Boolean get() = !state.won && state.stock.size >= 10 && state.columns.all { it.isNotEmpty() }

    fun selectable(column: Int, card: Int): Boolean {
        val cards = state.columns.getOrNull(column) ?: return false
        if (state.won || card !in cards.indices || !cards[card].faceUp) return false
        return (card until cards.lastIndex).all { cards[it].rank == cards[it + 1].rank + 1 && cards[it + 1].faceUp }
    }
    fun move(from: Int, card: Int, to: Int): Boolean {
        if (from == to || to !in 0..9 || !selectable(from,card)) return false
        val target = state.columns[to]
        val source = state.columns[from]
        if (target.isNotEmpty() && target.last().rank != source[card].rank + 1) return false
        remember()
        val columns = state.columns.map { it.toMutableList() }
        columns[to].addAll(columns[from].subList(card,columns[from].size).toList())
        columns[from].subList(card,columns[from].size).clear()
        expose(columns[from])
        state = settle(columns,state.stock,state.moves + 1)
        return true
    }
    fun deal(): Boolean {
        if (!canDeal) return false
        remember()
        val columns = state.columns.map { it.toMutableList() }
        for (column in 0..9) columns[column].add(SpiderCard(state.stock[column],true))
        state = settle(columns,state.stock.drop(10),state.moves + 1)
        return true
    }
    fun undo(): Boolean {
        if (history.isEmpty()) return false
        state = history.removeLast()
        return true
    }
    fun hint(): SpiderMove? {
        if (state.won) return null
        // Prefer moves onto another card; empty-column moves are offered afterwards.
        for (empty in listOf(false,true)) for (from in 0..9) {
            for (card in state.columns[from].indices) {
                if (!selectable(from,card)) continue
                for (to in 0..9) {
                    if (to == from || state.columns[to].isEmpty() != empty) continue
                    if (empty) {
                        // Moving an entire column to an empty column changes nothing useful.
                        if (card > 0) return SpiderMove(from,card,to)
                    } else if (state.columns[to].last().rank == state.columns[from][card].rank + 1) return SpiderMove(from,card,to)
                }
            }
        }
        return null
    }
    fun save(): String = (listOf(state) + history.toList()).joinToString("~",transform = ::encode)
    private fun remember() {
        if (history.size >= 64) history.removeFirst()
        history.addLast(state)
    }
    private fun expose(cards: MutableList<SpiderCard>) {
        if (cards.isNotEmpty() && !cards.last().faceUp) cards[cards.lastIndex] = cards.last().copy(faceUp = true)
    }
    private fun settle(columns: List<MutableList<SpiderCard>>,stock: List<Int>,moves: Int): SpiderState {
        var completed = state.completed
        for (cards in columns) {
            while (cards.size >= 13 && cards.takeLast(13).withIndex().all { (index,card) -> card.faceUp && card.rank == 13-index }) {
                cards.subList(cards.size-13,cards.size).clear()
                completed++
                expose(cards)
            }
        }
        return SpiderState(columns.map { it.toList() },stock.toList(),completed,moves)
    }
    companion object {
        private fun initial(seed: Long): SpiderState {
            val deck = (1..13).flatMap { rank -> List(8) { rank } }.shuffled(Random(seed))
            var next = 0
            val columns = List(10) { column ->
                val count = if (column < 4) 6 else 5
                List(count) { index -> SpiderCard(deck[next++],index == count-1) }
            }
            return SpiderState(columns,deck.drop(54))
        }
        private fun encode(state: SpiderState): String = "SP1|${state.completed}|${state.moves}|${state.stock.joinToString(",")}|" +
            state.columns.joinToString(";") { column -> column.joinToString(",") { if(it.faceUp) it.rank.toString() else (-it.rank).toString() } }
        fun restore(value: String): SpiderEngine? = runCatching {
            require(value.length in 1..60000)
            val parts = value.split('~')
            require(parts.size <= 65)
            val snapshots = parts.map(::decode)
            SpiderEngine(0).apply {
                state = snapshots.first()
                snapshots.drop(1).forEach(history::addLast)
            }
        }.getOrNull()
        private fun decode(value: String): SpiderState {
            val p = value.split('|')
            require(p.size == 5 && p[0] == "SP1")
            val completed = p[1].toInt(); val moves = p[2].toInt()
            require(completed in 0..8 && moves in 0..1_000_000)
            val stock = if (p[3].isEmpty()) emptyList() else p[3].split(',').map(String::toInt)
            require(stock.size <= 50 && stock.size % 10 == 0 && stock.all { it in 1..13 })
            val columns = p[4].split(';').map { raw ->
                if (raw.isEmpty()) emptyList() else raw.split(',').map { token ->
                    val rank = token.toInt(); require(rank in -13..13 && rank != 0)
                    SpiderCard(kotlin.math.abs(rank),rank > 0)
                }
            }
            require(columns.size == 10)
            require(columns.all { cards ->
                cards.size <= 104 && (cards.isEmpty() || cards.last().faceUp) &&
                    cards.zipWithNext().none { (a,b) -> a.faceUp && !b.faceUp }
            })
            val all = columns.flatten().map { it.rank } + stock
            require(all.size == 104-completed*13)
            require((1..13).all { rank -> all.count { it == rank } == 8-completed })
            return SpiderState(columns,stock,completed,moves)
        }
    }
}
