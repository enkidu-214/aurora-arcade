package com.aurora.arcade.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.arcade.GameSession
import com.aurora.arcade.MiniGameStore
import com.aurora.arcade.core.games.SpiderEngine
import com.aurora.arcade.core.games.SpiderMove
import com.aurora.arcade.ui.*

@Composable
fun SpiderScreen(session: GameSession, saves: MiniGameStore) {
    var engine by remember { mutableStateOf(saves.load("spider")?.let(SpiderEngine::restore) ?: SpiderEngine()) }
    var state by remember { mutableStateOf(engine.state) }
    var selected by remember { mutableStateOf<Pair<Int,Int>?>(null) }
    var hint by remember { mutableStateOf<SpiderMove?>(null) }
    var message by remember { mutableStateOf("点选连续牌组，再点目标列") }
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    fun publish() {
        state = engine.state
        saves.save("spider",engine.save())
        saves.record("spider",state.completed)
        selected = null; hint = null
        message = if (state.won) "八组全部收齐！" else "点选连续牌组，再点目标列"
        session.miniFeedback(state.won)
    }
    fun restart() {
        engine = SpiderEngine(); state = engine.state
        selected = null; hint = null
        message = "新牌局 · 点选连续牌组，再点目标列"
        saves.save("spider",engine.save())
    }
    fun tap(column: Int,index: Int?) {
        if(session.paused || state.won) return
        val source = selected
        if(source != null && source.first != column) {
            if(engine.move(source.first,source.second,column)) { publish(); return }
            message = "目标牌要比牌组首张大 1；空列可放任意连续牌组"
        }
        if(index != null && engine.selectable(column,index)) {
            selected = if(source == (column to index)) null else column to index
            hint = null
            message = selected?.let { "已选第 ${column+1} 列 ${spiderRank(state.columns[column][index].rank)}，点目标列" }
                ?: "点选连续牌组，再点目标列"
        }
    }
    DisposableEffect(saves) { onDispose { saves.save("spider",engine.save()) } }
    LaunchedEffect(session.paused) { if(session.paused) saves.save("spider",engine.save()) }
    LaunchedEffect(hint) { hint?.let { horizontal.animateScrollTo((it.from * 58 * horizontal.maxValue / 580).coerceIn(0,horizontal.maxValue)) } }
    MiniGameScaffold("蜘蛛纸牌 · 单花色",session,
        "已收 ${state.completed}/8 组 · ${state.moves} 步 · 最佳 ${saves.best("spider")} 组",
        "104 张单花色牌，分为十列和五次发牌。\n点选正面朝上且从大到小连续的牌组，再点目标列。目标末张必须比牌组首张大 1；空列可放任意连续牌组。移走末张后会翻开下一张。\n一列末尾集齐 K 到 A 的完整连续牌组会自动收走，收齐八组获胜。\n每次发牌向十列各发一张；有空列时不能发牌。左右滑动查看十列，上下滑动查看长列。提示会标出起点和目标，撤销可回退最近 64 步。",
        ::restart) {
        Text(message,color = if(state.won) Mint else GameText,fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp,vertical = 4.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF102A28),RoundedCornerShape(14.dp))) {
            val columnWidth = maxOf(52.dp,(maxWidth-20.dp)/10)
            val boardWidth = columnWidth*10+20.dp
            val columnHeights = state.columns.map { cards ->
                if(cards.isEmpty()) 130.dp else cards.dropLast(1).fold(24.dp) { offset,card -> offset + if(card.faceUp) 29.dp else 13.dp } + 72.dp
            }
            val boardHeight = maxOf(maxHeight,columnHeights.maxOrNull() ?: 130.dp)
            Box(Modifier.fillMaxSize().horizontalScroll(horizontal).verticalScroll(vertical)) {
                Row(Modifier.width(boardWidth).height(boardHeight).padding(horizontal = 10.dp)) {
                    state.columns.forEachIndexed { column,cards ->
                        val target = hint?.to == column
                        Box(Modifier.width(columnWidth).fillMaxHeight().padding(horizontal = 2.dp)
                            .semantics { contentDescription = "第 ${column+1} 列" }
                            .border(if(target) 2.dp else 1.dp,if(target) Color(0xFFFFCB76) else Color(0xFF294B49),RoundedCornerShape(8.dp))
                            .clickable(enabled = !session.paused && !state.won) { tap(column,null) }) {
                            Text("${column+1}",color = if(target) Color(0xFFFFCB76) else GameMuted,
                                fontSize = 12.sp,modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp))
                            var offset = 25.dp
                            cards.forEachIndexed { index,card ->
                                val y = offset
                                val marked = selected?.let { it.first == column && index >= it.second } == true
                                val sourceHint = hint?.let { it.from == column && it.card == index } == true
                                Box(Modifier.offset(y = y).fillMaxWidth().height(68.dp)
                                    .background(if(card.faceUp) Color(0xFFE6EFE9) else Color(0xFF416B73),RoundedCornerShape(6.dp))
                                    .border(if(marked || sourceHint) 2.dp else 1.dp,
                                        if(marked || sourceHint) Mint else Color(0xFF173D42),RoundedCornerShape(6.dp))
                                    .clickable(enabled = !session.paused && !state.won) { tap(column,index) }) {
                                    if(card.faceUp) {
                                        Text(spiderRank(card.rank),fontWeight = FontWeight.Bold,fontSize = 17.sp,
                                            color = Color(0xFF153E36),modifier = Modifier.padding(start = 5.dp,top = 2.dp))
                                        Text("♠",fontSize = 19.sp,color = Color(0xFF153E36),modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                                    } else Text("· ·",color = Color(0xFF9EBFC1),fontSize = 13.sp,modifier = Modifier.align(Alignment.TopCenter))
                                }
                                offset += if(card.faceUp) 29.dp else 13.dp
                            }
                            if(cards.isEmpty()) Text("空列",fontSize = 12.sp,color = GameMuted,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 52.dp))
                        }
                    }
                }
            }
        }
        Text("横向滑动查看全部 10 列 · 纵向滑动查看长列",color = GameMuted,fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp,horizontal = 4.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniAction("提示",!session.paused && !state.won,Modifier.weight(1f)) {
                if(!session.paused) {
                    hint = engine.hint()
                    selected = hint?.let { it.from to it.card }
                    message = hint?.let { "第 ${it.from+1} 列 ${spiderRank(state.columns[it.from][it.card].rank)} → 第 ${it.to+1} 列" }
                        ?: if(engine.canDeal) "可以发一行新牌" else "暂无移动提示，可撤销或重新开始"
                }
            }
            MiniAction("撤销",!session.paused && engine.canUndo,Modifier.weight(1f)) {
                if(!session.paused && engine.undo()) publish()
            }
            MiniAction(if(state.won) "再来一局" else "发牌 ${state.stock.size/10}",
                !session.paused && (state.won || engine.canDeal),Modifier.weight(1f)) {
                if(!session.paused) { if(state.won) restart() else if(engine.deal()) publish() }
            }
        }
        if(!state.won && state.stock.isNotEmpty() && state.columns.any { it.isEmpty() })
            Text("先填满空列才能发牌",color = Color(0xFFFFCB76),fontSize = 12.sp,modifier = Modifier.padding(4.dp))
    }
}

private fun spiderRank(rank: Int): String = when(rank) { 1 -> "A"; 11 -> "J"; 12 -> "Q"; 13 -> "K"; else -> rank.toString() }
