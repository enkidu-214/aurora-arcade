package com.aurora.arcade.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aurora.arcade.GameSession
import com.aurora.arcade.MiniGameStore
import com.aurora.arcade.core.games.SnakeDirection
import com.aurora.arcade.core.games.SnakeEngine
import kotlin.math.abs

@Composable fun SnakeScreen(session:GameSession,saves:MiniGameStore) {
 var engine by remember { mutableStateOf(SnakeEngine.restore(saves.load("snake"))?:SnakeEngine()) }
 var state by remember(engine) { mutableStateOf(engine.state) }
 var previousBody by remember(engine) { mutableStateOf(engine.state.body) }
 var transitionNanos by remember(engine) { mutableLongStateOf(0L) }
 var drawNanos by remember(engine) { mutableLongStateOf(0L) }
 val reducedMotion=session.settings.reducedMotion
 var best by remember { mutableIntStateOf(saves.best("snake")) }
 fun save() {saves.save("snake",engine.save());saves.record("snake",engine.state.score);best=maxOf(best,engine.state.score)}
 fun turn(direction:SnakeDirection) {if(!session.paused && engine.turn(direction))session.miniFeedback()}
 val currentEngine by rememberUpdatedState(engine)
 DisposableEffect(Unit) {onDispose {val saved=currentEngine;saves.save("snake",saved.save());saves.record("snake",saved.state.score)}}
 LaunchedEffect(session.paused,engine) {
  if(session.paused){save();return@LaunchedEffect}
  var previous=0L;var savedElapsed=0L
  while(!engine.state.over) {withFrameNanos {now->
   drawNanos=now
   if(previous!=0L) {
    val delta=((now-previous)/1_000_000).coerceIn(0,50);val old=engine.state
    if(engine.advance(delta)) {previousBody=old.body;transitionNanos=now;state=engine.state;if(state.score!=old.score || state.over!=old.over) {session.miniFeedback(state.score!=old.score);save()}}
    savedElapsed+=delta;if(savedElapsed>=2000){save();savedElapsed=0}
   };previous=now
  }}
 }
 val result=if(state.won)"全部吃满 · 完美通关" else if(state.over)"这一局结束 · 随时再来一局" else if(state.fixed)"固定速度 · 从容转弯" else "经典加速 · 越吃越快"
 MiniGameScaffold("贪吃蛇",session,"${state.score} 分  ·  最佳 ${maxOf(best,state.score)}  ·  ${state.body.size} 格","滑动棋盘或点击方向键转弯。吃到亮色食物会长一格；撞墙或撞到身体结束。不能直接反向，可提前缓存两次转弯。经典模式会逐渐加速，固定模式保持舒缓速度。",{val fixed=state.fixed;engine=SnakeEngine().also{it.setFixed(fixed)};state=engine.state;save()}) {
  Text(result,color=if(state.over)Color(0xFFFFC078) else Color(0xFF95AAA5),modifier=Modifier.padding(vertical=4.dp))
  BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
   val cell=minOf(maxWidth/18,maxHeight/24)
   Canvas(Modifier.width(cell*18).height(cell*24).background(Color(0xFF101F25)).semantics {contentDescription="贪吃蛇棋盘，${state.body.size} 格，${state.score} 分"}
    .pointerInput(engine,session.paused) {
     var drag=Offset.Zero
     detectDragGestures(onDragStart={drag=Offset.Zero},onDrag={change,amount->
      change.consume();drag+=amount
      if(abs(drag.x)>24 || abs(drag.y)>24) {turn(if(abs(drag.x)>abs(drag.y))if(drag.x>0)SnakeDirection.RIGHT else SnakeDirection.LEFT else if(drag.y>0)SnakeDirection.DOWN else SnakeDirection.UP);drag=Offset.Zero}
     })
    }) {
    val unit=size.width/18
    for(x in 0..18)drawLine(Color(0xFF1A3036),Offset(x*unit,0f),Offset(x*unit,size.height),1f)
    for(y in 0..24)drawLine(Color(0xFF1A3036),Offset(0f,y*unit),Offset(size.width,y*unit),1f)
    if(state.food>=0) {val pos=Offset((state.food%18+.5f)*unit,(state.food/18+.5f)*unit);drawCircle(Color(0xFFFFBD70).copy(alpha=.13f),unit*.49f,pos);drawCircle(Color(0xFFFFBD70),unit*.32f,pos)}
    val progress=if(reducedMotion || state.over || transitionNanos==0L)1f else ((drawNanos-transitionNanos)/(minOf(engine.interval,100)*1_000_000f)).coerceIn(0f,1f)
    state.body.indices.reversed().forEach {index->
     val at=state.body[index];val from=previousBody.getOrElse(index){previousBody.last()};val x=(from%18+(at%18-from%18)*progress)*unit;val y=(from/18+(at/18-from/18)*progress)*unit
     drawRoundRect(if(index==0)Color(0xFFB2FFE4) else Color(0xFF56CEA7),Offset(x+2,y+2),Size(unit-4,unit-4),androidx.compose.ui.geometry.CornerRadius(unit*.2f))
    }
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   MiniAction(if(state.over)"再来一局" else if(state.fixed)"速度：固定" else "速度：加速",enabled=!session.paused,modifier=Modifier.weight(1f)) {if(!session.paused){if(state.over){val fixed=state.fixed;engine=SnakeEngine().also{it.setFixed(fixed)}}else engine.setFixed(!state.fixed);state=engine.state;save()}}
   MiniAction("↑",enabled=!session.paused && !state.over,modifier=Modifier.weight(1f)) {turn(SnakeDirection.UP)}
  }
  Row(Modifier.fillMaxWidth().padding(top=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   MiniAction("←",enabled=!session.paused && !state.over,modifier=Modifier.weight(1f)) {turn(SnakeDirection.LEFT)}
   MiniAction("↓",enabled=!session.paused && !state.over,modifier=Modifier.weight(1f)) {turn(SnakeDirection.DOWN)}
   MiniAction("→",enabled=!session.paused && !state.over,modifier=Modifier.weight(1f)) {turn(SnakeDirection.RIGHT)}
  }
 }
}
