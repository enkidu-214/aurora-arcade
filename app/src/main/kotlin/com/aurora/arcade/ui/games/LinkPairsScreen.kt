package com.aurora.arcade.ui.games

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.arcade.GameSession
import com.aurora.arcade.MiniGameStore
import com.aurora.arcade.core.games.LinkPairsEngine
import com.aurora.arcade.core.games.LinkPoint
import com.aurora.arcade.ui.*
import kotlinx.coroutines.delay

@Composable
fun LinkPairsScreen(session: GameSession,saves: MiniGameStore) {
    var engine by remember { mutableStateOf(saves.load("link")?.let(LinkPairsEngine::restore) ?: LinkPairsEngine()) }
    var state by remember { mutableStateOf(engine.state) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var hint by remember { mutableStateOf<Pair<Int,Int>?>(null) }
    var connection by remember { mutableStateOf<List<LinkPoint>>(emptyList()) }
    var connectionId by remember { mutableIntStateOf(0) }
    val connectionAlpha = remember { Animatable(0f) }
    var message by remember { mutableStateOf("点两块相同数字，用不超过两个转角的线连接") }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD } }
    val colors = remember { listOf(0xFFA0E6C8,0xFF91D5EF,0xFFF3C281,0xFFC3ADF0,0xFFF2ACBE,0xFFCBD989,
        0xFF8DD5D3,0xFFD5B9A0,0xFFB9CDED,0xFFE9D78C,0xFFEBAA93,0xFFB1D7AF).map { Color(it.toInt()) } }
    fun persist() { saves.save("link",engine.save()); saves.record("link",state.width*state.height-state.remaining) }
    fun restart() {
        engine = LinkPairsEngine(); state = engine.state
        selected = null; hint = null; connection = emptyList(); connectionId++
        message = "新棋盘 · 没有倒计时，慢慢寻找"
        persist()
    }
    fun tap(index: Int) {
        if(session.paused || state.won || index !in state.tiles.indices || state.tiles[index] == 0) return
        val previous = selected
        hint = null
        if(previous == index) { selected = null; return }
        if(previous != null) {
            val path = engine.match(previous,index)
            if(path != null) {
                val reordered = state.tiles.indices.any { it != previous && it != index && state.tiles[it] != engine.state.tiles[it] }
                state = engine.state; selected = null
                connection = if(reordered) emptyList() else path; connectionId++
                message = when { state.won -> "全部配对完成！"; reordered -> "已配对 · 剩余方块无路可走，已自动重排"; else -> "已连成一对 · 继续寻找相同数字" }
                persist(); session.miniFeedback(state.won)
                return
            }
            message = if(state.tiles[previous] != state.tiles[index]) "选择两块相同数字" else "这两块被挡住了，试试别的配对"
        }
        selected = index
    }
    DisposableEffect(saves) { onDispose { saves.save("link",engine.save()) } }
    LaunchedEffect(session.paused) { if(session.paused) saves.save("link",engine.save()) }
    LaunchedEffect(connectionId) {
        if(connection.isNotEmpty()) {
            connectionAlpha.snapTo(1f)
            if(session.settings.reducedMotion) { delay(220); connectionAlpha.snapTo(0f) }
            else connectionAlpha.animateTo(0f,tween(500))
        }
    }
    MiniGameScaffold("连连看",session,
        "剩余 ${state.remaining} 块 · 已连 ${state.moves} 对 · 最佳 ${saves.best("link")/2} 对",
        "点两块相同数字。如果它们能用最多两个转角的线连接，且线没有穿过其他方块，就能消除。连线可以从棋盘外围绕过去。\n没有强制倒计时，清空棋盘即获胜。提示会标出可消的一对；重排会保留剩余方块及空位，并保证有路可走。消除后如果棋盘无路可走，会自动重排剩余方块。\n进度会自动保存，可以随时暂停或返回。",
        ::restart) {
        Text(message,color = if(state.won) Mint else GameText,fontSize = 13.sp,modifier = Modifier.padding(horizontal = 6.dp,vertical = 5.dp))
        Canvas(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF12222C),RoundedCornerShape(14.dp))
            .semantics { contentDescription = "连连看棋盘，剩余 ${state.remaining} 块" }
            .pointerInput(engine,session.paused,state) {
                detectTapGestures { offset ->
                    if(!session.paused) {
                        val tile = minOf(size.width.toFloat()/(state.width+2),size.height.toFloat()/(state.height+2))
                        val left = (size.width-tile*(state.width+2))/2
                        val top = (size.height-tile*(state.height+2))/2
                        val x = kotlin.math.floor((offset.x-left)/tile-1).toInt()
                        val y = kotlin.math.floor((offset.y-top)/tile-1).toInt()
                        if(x in 0 until state.width && y in 0 until state.height) tap(y*state.width+x)
                    }
                }
            }) {
            val tile = minOf(size.width/(state.width+2),size.height/(state.height+2))
            val left = (size.width-tile*(state.width+2))/2
            val top = (size.height-tile*(state.height+2))/2
            val inset = minOf(3.dp.toPx(),tile*.075f)
            fun center(point: LinkPoint) = Offset(left+(point.x+1.5f)*tile,top+(point.y+1.5f)*tile)
            paint.textSize = minOf(19.sp.toPx(),tile*.48f)
            paint.color = android.graphics.Color.rgb(21,43,52)
            state.tiles.forEachIndexed { index,kind ->
                if(kind > 0) {
                    val origin = Offset(left+(index%state.width+1)*tile+inset,top+(index/state.width+1)*tile+inset)
                    val square = Size(tile-inset*2,tile-inset*2)
                    drawRoundRect(colors[kind-1],origin,square,CornerRadius(8.dp.toPx()))
                    val highlighted = selected == index || hint?.let { index == it.first || index == it.second } == true
                    if(highlighted) drawRoundRect(Color(0xFFFFFAE7),origin,square,CornerRadius(8.dp.toPx()),style = Stroke(3.dp.toPx()))
                    val point = center(LinkPoint(index%state.width,index/state.width))
                    drawContext.canvas.nativeCanvas.drawText(kind.toString(),point.x,point.y-(paint.ascent()+paint.descent())/2,paint)
                }
            }
            if(connection.isNotEmpty() && connectionAlpha.value > 0f) {
                val path = Path()
                connection.forEachIndexed { index,point ->
                    val p = center(point)
                    if(index == 0) path.moveTo(p.x,p.y) else path.lineTo(p.x,p.y)
                }
                drawPath(path,Color(0xFFFFE3A6).copy(alpha = connectionAlpha.value),style = Stroke(4.dp.toPx()))
            }
            if(state.won) {
                paint.color = android.graphics.Color.rgb(156,244,204); paint.textSize = 26.sp.toPx()
                drawContext.canvas.nativeCanvas.drawText("配对完成",size.width/2,size.height/2,paint)
            }
        }
        Text("无倒计时 · 连线最多两个转角 · 可从外围绕行",color = GameMuted,fontSize = 11.sp,modifier = Modifier.padding(horizontal = 4.dp,vertical = 6.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniAction("提示",!session.paused && !state.won,Modifier.weight(1f)) {
                if(!session.paused) { hint = engine.hint(); selected = null; message = "亮边的两块可以连接" }
            }
            MiniAction(if(state.won) "再来一局" else "重排",!session.paused,Modifier.weight(1f)) {
                if(!session.paused) {
                    if(state.won) restart()
                    else if(engine.reshuffle()) {
                        state = engine.state; selected = null; hint = null; connection = emptyList(); connectionId++
                        message = "已重排 · 每一对都有机会连接"; persist(); session.miniFeedback()
                    }
                }
            }
        }
    }
}
