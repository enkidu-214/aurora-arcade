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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun SokobanScreen(session:GameSession,saves:MiniGameStore) {
    var engine by remember {mutableStateOf(SokobanEngine.restore(saves.load("sokoban")))}
    var state by remember {mutableStateOf(engine.state)}
    var fromState by remember {mutableStateOf(state)}
    var revision by remember {mutableIntStateOf(0)}
    val slide=remember {Animatable(1f)}
    var progress by remember {mutableIntStateOf(saves.best("sokoban"))}
    val currentEngine by rememberUpdatedState(engine)
    fun persist() {saves.save("sokoban",engine.save());if(state.complete){saves.record("sokoban",state.level+1);progress=maxOf(progress,state.level+1)}}
    fun refresh(animated:Boolean=false) {val old=state;state=engine.state;fromState=if(animated)old else state;revision++;persist()}
    fun restart() {engine.reset();refresh()}
    fun move(direction:SokobanDirection) {if(!session.paused && engine.move(direction)){refresh(true);session.miniFeedback(state.complete)}}
    DisposableEffect(Unit) {onDispose {saves.save("sokoban",currentEngine.save())}}
    LaunchedEffect(session.paused) {if(session.paused)persist()}
    LaunchedEffect(revision,session.settings.reducedMotion) {
        if(!session.settings.reducedMotion && fromState.level==state.level && fromState!=state) {
            slide.snapTo(0f);slide.animateTo(1f,tween(105))
        } else slide.snapTo(1f)
    }
    MiniGameScaffold("推箱子",session,"第 ${state.level+1} / ${SokobanEngine.levelCount} 关 · ${state.moves} 步 · 推动 ${state.pushes} 次 · 已通 $progress 关",
        "把全部木箱推到绿色目标点上。每次只能推一个箱子，不能拉箱子，也不能穿过墙壁。点方向键移动，或在棋盘上滑动。把箱子推入没有目标的角落会卡住，可以撤回或重置本关。完成后点下一关；共八关，逐渐增加箱子和障碍。撤回最多保留 200 步，离开后仍然可用。",::restart) {
        Canvas(Modifier.weight(1f).fillMaxWidth().semantics {contentDescription="推箱子棋盘，滑动或用方向键把箱子推到绿色目标"}
            .pointerInput(session.paused,state.level) {
                var drag=Offset.Zero
                detectDragGestures(onDragStart={drag=Offset.Zero},onDragCancel={drag=Offset.Zero},onDragEnd={
                    if(drag.getDistance()>24.dp.toPx())move(if(abs(drag.x)>abs(drag.y)) {if(drag.x>0)SokobanDirection.RIGHT else SokobanDirection.LEFT} else if(drag.y>0)SokobanDirection.DOWN else SokobanDirection.UP)
                }) {change,amount->change.consume();drag+=amount}
            }) {
            val cell=minOf(size.width/state.width,size.height/state.height)
            val origin=Offset((size.width-state.width*cell)/2,(size.height-state.height*cell)/2)
            val gap=3.dp.toPx();val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Ink.toArgb();textSize=cell*.36f;textAlign=Paint.Align.CENTER;typeface=Typeface.create("sans-serif",Typeface.BOLD)}
            repeat(state.width*state.height) {i->
                val p=origin+Offset(i%state.width*cell,i/state.width*cell);val center=p+Offset(cell/2,cell/2)
                if(i !in state.floor) {
                    drawRoundRect(GameFrame,p+Offset(gap/2,gap/2),Size(cell-gap,cell-gap),CornerRadius(5.dp.toPx()))
                    drawLine(GameFrameLight.copy(alpha=.45f),p+Offset(gap,cell*.4f),p+Offset(cell-gap,cell*.4f),1.dp.toPx())
                } else {
                    drawRoundRect(GamePanel,p+Offset(gap/2,gap/2),Size(cell-gap,cell-gap),CornerRadius(5.dp.toPx()))
                    if(i in state.goals) {drawCircle(Mint.copy(alpha=.2f),cell*.3f,center);drawCircle(Mint,cell*.18f,center,style=Stroke(2.dp.toPx()))}
                }
            }
            val fraction=if(session.settings.reducedMotion)1f else slide.value
            val boxFrom=(fromState.boxes-state.boxes).firstOrNull()
            val boxTo=(state.boxes-fromState.boxes).firstOrNull()
            fun point(index:Int)=origin+Offset(index%state.width*cell,index/state.width*cell)
            state.boxes.forEach {i->
                val target=point(i)
                val p=if(i==boxTo && boxFrom!=null)point(boxFrom)+(target-point(boxFrom))*fraction else target
                val box=if(i in state.goals)Mint else Color(0xFFD9B38C)
                drawRoundRect(box,p+Offset(cell*.14f,cell*.14f),Size(cell*.72f,cell*.72f),CornerRadius(6.dp.toPx()))
                drawRoundRect(Ink.copy(alpha=.25f),p+Offset(cell*.24f,cell*.24f),Size(cell*.52f,cell*.52f),CornerRadius(2.dp.toPx()),style=Stroke(2.dp.toPx()))
                drawLine(Ink.copy(alpha=.25f),p+Offset(cell*.25f,cell*.25f),p+Offset(cell*.75f,cell*.75f),2.dp.toPx())
                drawLine(Ink.copy(alpha=.25f),p+Offset(cell*.75f,cell*.25f),p+Offset(cell*.25f,cell*.75f),2.dp.toPx())
            }
            val playerTarget=point(state.player)
            val playerSource=if(fromState.level==state.level)point(fromState.player) else playerTarget
            val center=playerSource+(playerTarget-playerSource)*fraction+Offset(cell/2,cell/2)
            // A small compass token marks the player, with no face or emoji.
            val token=Path().apply {moveTo(center.x,center.y-cell*.31f);lineTo(center.x+cell*.27f,center.y);lineTo(center.x,center.y+cell*.31f);lineTo(center.x-cell*.27f,center.y);close()}
            drawPath(token,Color(0xFF9FC9EF));drawContext.canvas.nativeCanvas.drawText("你",center.x,center.y-(paint.ascent()+paint.descent())/2,paint)
        }

        Text(if(state.complete)if(state.level==SokobanEngine.levelCount-1)"八关全部完成！可以撤回重玩，或回到第一关。" else "全部箱子已就位 · 下一关等着你" else "绿色圆点是目标 · 蓝色菱形是你",color=if(state.complete)Mint else GameMuted,fontSize=12.sp,modifier=Modifier.padding(vertical=6.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("←" to SokobanDirection.LEFT,"↑" to SokobanDirection.UP,"↓" to SokobanDirection.DOWN,"→" to SokobanDirection.RIGHT).forEach {(label,d)->MiniAction(label,!session.paused && !state.complete,Modifier.weight(1f).height(56.dp).semantics {contentDescription=when(d){SokobanDirection.LEFT->"向左";SokobanDirection.UP->"向上";SokobanDirection.DOWN->"向下";SokobanDirection.RIGHT->"向右"}}){move(d)}}
        }
        Row(Modifier.fillMaxWidth().padding(top=6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            MiniAction("撤回",!session.paused && engine.canUndo,Modifier.weight(1f)){if(!session.paused && engine.undo())refresh(true)}
            MiniAction("重置",!session.paused,Modifier.weight(1f)){if(!session.paused)restart()}
            MiniAction(if(state.level==SokobanEngine.levelCount-1)"第一关" else "下一关",!session.paused && state.complete,Modifier.weight(1f)) {
                if(!session.paused && state.complete){if(state.level==SokobanEngine.levelCount-1)engine=SokobanEngine() else engine.nextLevel();refresh()}
            }
        }
    }
}
