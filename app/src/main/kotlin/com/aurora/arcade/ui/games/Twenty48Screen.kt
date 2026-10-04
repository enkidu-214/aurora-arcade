package com.aurora.arcade.ui.games

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.arcade.GameSession
import com.aurora.arcade.MiniGameStore
import com.aurora.arcade.core.games.*
import com.aurora.arcade.ui.*
import kotlin.math.abs

@Composable
fun Twenty48Screen(session:GameSession,saves:MiniGameStore) {
    var engine by remember { mutableStateOf(Twenty48Engine.restore(saves.load("2048"))) }
    var state by remember { mutableStateOf(engine.state) }
    var revision by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(saves.best("2048")) }
    val currentEngine by rememberUpdatedState(engine)
    val slide=remember { Animatable(1f) }
    fun persist() { saves.save("2048",engine.save());saves.record("2048",state.score);best=maxOf(best,state.score) }
    fun refresh() { state=engine.state;revision++;persist() }
    fun move(direction:Twenty48Direction) {
        val hadWon=state.won
        if(!session.paused && engine.move(direction)) {refresh();session.miniFeedback(state.won && !hadWon)}
    }
    fun restart() {engine=Twenty48Engine();refresh()}
    DisposableEffect(Unit) { onDispose {saves.save("2048",currentEngine.save())} }
    LaunchedEffect(session.paused) {if(session.paused)persist()}
    LaunchedEffect(revision,session.settings.reducedMotion) {
        if(engine.trails.isNotEmpty() && !session.settings.reducedMotion) {slide.snapTo(0f);slide.animateTo(1f,tween(130))} else slide.snapTo(1f)
    }
    MiniGameScaffold("2048",session,"得分 ${state.score}  ·  最高 $best",
        "在棋盘上向任意方向滑动，或点下方方向键。相同数字相遇会合并，每块每步只合并一次。有效移动后出现一个新数字。合成 2048 后可以继续挑战；棋盘无法移动时结束。撤回可以恢复上一步，随机新块也会一起撤回。",::restart) {
        Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
            Canvas(Modifier.fillMaxSize().semantics {contentDescription="2048 棋盘，滑动合并数字"}
                .pointerInput(session.paused) {
                    var drag=Offset.Zero
                    detectDragGestures(onDragStart={drag=Offset.Zero},onDragCancel={drag=Offset.Zero},
                        onDragEnd={
                            if(drag.getDistance()>24.dp.toPx())move(if(abs(drag.x)>abs(drag.y)) {
                                if(drag.x>0)Twenty48Direction.RIGHT else Twenty48Direction.LEFT
                            } else if(drag.y>0)Twenty48Direction.DOWN else Twenty48Direction.UP)
                        }) {change,amount->change.consume();drag+=amount}
                }) {
                val edge=minOf(size.width,size.height)-12.dp.toPx();val cell=edge/4f;val gap=6.dp.toPx()
                val origin=Offset((size.width-edge)/2f,(size.height-edge)/2f)
                drawRoundRect(GamePanel,origin-Offset(gap/2,gap/2),Size(edge+gap,edge+gap),CornerRadius(16.dp.toPx()))
                repeat(16) {i->drawRoundRect(GameBackground,origin+Offset(i%4*cell+gap/2,i/4*cell+gap/2),Size(cell-gap,cell-gap),CornerRadius(10.dp.toPx()))}
                val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {textAlign=Paint.Align.CENTER;typeface=Typeface.create("sans-serif",Typeface.BOLD)}
                fun tile(value:Int,position:Offset,scale:Float=1f,alpha:Float=1f) {
                    val colors=listOf(Color(0xFFB4DACA),Color(0xFF97C8D8),Color(0xFF81ACDB),Color(0xFFAD9ADB),Color(0xFFD69DBB),Color(0xFFE7B294),Color(0xFFE1C679),Color(0xFFF1D15F))
                    val color=colors[(Integer.numberOfTrailingZeros(value)-1).coerceIn(0,colors.lastIndex)]
                    val tileSize=(cell-gap)*scale;val inset=(cell-tileSize)/2f
                    drawRoundRect(color.copy(alpha=alpha),position+Offset(inset,inset),Size(tileSize,tileSize),CornerRadius(10.dp.toPx()))
                    paint.color=Ink.copy(alpha=alpha).toArgb();paint.textSize=cell*(if(value>=100000) .18f else if(value>=10000) .23f else if(value>=1000) .29f else .36f)*scale
                    drawContext.canvas.nativeCanvas.drawText(value.toString(),position.x+cell/2,position.y+cell/2-(paint.ascent()+paint.descent())/2,paint)
                }
                val progress=if(session.settings.reducedMotion)1f else slide.value
                if(progress<1f && engine.trails.isNotEmpty()) {
                    val merges=engine.trails.groupingBy {it.to}.eachCount().filterValues {it==2}.keys
                    val mergePhase=((progress-.68f)/.32f).coerceIn(0f,1f)
                    engine.trails.forEach {trail ->
                        val x=(trail.from%4+(trail.to%4-trail.from%4)*progress)*cell
                        val y=(trail.from/4+(trail.to/4-trail.from/4)*progress)*cell
                        tile(trail.value,origin+Offset(x,y),alpha=if(trail.to in merges)1f-mergePhase else 1f)
                    }
                    merges.forEach {target->if(mergePhase>0f)tile(state.cells[target],origin+Offset(target%4*cell,target/4*cell),.88f+.12f*mergePhase,mergePhase)}
                    val new=engine.spawned;if(new>=0)tile(state.cells[new],origin+Offset(new%4*cell,new/4*cell),.65f+.35f*progress)
                } else state.cells.forEachIndexed {i,value->if(value>0)tile(value,origin+Offset(i%4*cell,i/4*cell))}
            }
        }
        Text(when {state.over->"棋盘已满 · 撤回一步，或重新挑战";state.won->"已合成 2048！继续挑战更大的数字";else->"滑动棋盘，让相同数字相遇"},color=if(state.over || state.won)Mint else GameMuted,fontSize=12.sp,modifier=Modifier.padding(vertical=6.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("←" to Twenty48Direction.LEFT,"↑" to Twenty48Direction.UP,"↓" to Twenty48Direction.DOWN,"→" to Twenty48Direction.RIGHT).forEach {(label,d)->
                MiniAction(label,!session.paused && !state.over,Modifier.weight(1f).height(56.dp).semantics {contentDescription=when(d){Twenty48Direction.LEFT->"向左";Twenty48Direction.UP->"向上";Twenty48Direction.DOWN->"向下";Twenty48Direction.RIGHT->"向右"}}) {move(d)}
            }
        }
        Row(Modifier.fillMaxWidth().padding(top=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MiniAction("撤回一步",!session.paused && engine.canUndo,Modifier.weight(1f)) {if(!session.paused && engine.undo())refresh()}
            MiniAction("新一局",!session.paused,Modifier.weight(1f)) {if(!session.paused){if(state.over)restart() else session.pause()}}
        }
    }
}
