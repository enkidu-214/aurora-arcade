package com.aurora.arcade.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aurora.arcade.GameSession
import com.aurora.arcade.MiniGameStore
import com.aurora.arcade.core.games.BubbleEngine
import kotlin.math.PI
import kotlin.math.atan2

private data class BubbleFade(val row:Int,val col:Int,val color:Int,val falling:Boolean)

@Composable fun BubbleScreen(session:GameSession,saves:MiniGameStore) {
 var engine by remember {mutableStateOf(BubbleEngine.restore(saves.load("bubble"))?:BubbleEngine())}
 var state by remember(engine) {mutableStateOf(engine.state)}
 var fading by remember(engine){mutableStateOf(emptyList<BubbleFade>())}
 var clearNanos by remember(engine){mutableLongStateOf(0L)}
 var drawNanos by remember(engine){mutableLongStateOf(0L)}
 val reducedMotion=session.settings.reducedMotion
 var best by remember {mutableIntStateOf(saves.best("bubble"))}
 var angle by remember(engine) {mutableDoubleStateOf(-PI/2)}
 fun save(){saves.save("bubble",engine.save());saves.record("bubble",engine.state.score);best=maxOf(best,engine.state.score)}
 fun aim(at:Offset,width:Float){if(!session.paused && engine.state.projectile==null){val unit=width/1000;angle=atan2((at.y/unit-1300.0).coerceAtMost(-60.0),at.x/unit-500.0).coerceIn(-PI+.20,-.20)}}
 fun shoot(){if(!session.paused && engine.launch(angle)){state=engine.state;session.miniFeedback();save()}}
 val currentEngine by rememberUpdatedState(engine)
 DisposableEffect(Unit){onDispose{val saved=currentEngine;saves.save("bubble",saved.save());saves.record("bubble",saved.state.score)}}
 LaunchedEffect(session.paused,engine){
  if(session.paused){save();return@LaunchedEffect};var previous=0L;var savedElapsed=0L
  while(!engine.state.over || (clearNanos!=0L && drawNanos-clearNanos<420_000_000)){withFrameNanos{now->
   drawNanos=now
   if(previous!=0L){val delta=((now-previous)/1_000_000).coerceIn(0,50);val old=engine.state;val oldShot=old.projectile
    if(engine.advance(delta)){state=engine.state;if(oldShot!=null && state.projectile==null){
      if(state.score>old.score && !reducedMotion){fading=old.board.indices.filter{old.board[it]>=0 && state.board[it]<0}.map{BubbleFade(it/9,it%9,old.board[it],old.board[it]!=oldShot.color)};clearNanos=now}
      session.miniFeedback(state.score>old.score);save()}}
    savedElapsed+=delta;if(savedElapsed>=2000){save();savedElapsed=0}
   };previous=now
  }}
 }
 val colors=listOf(Color(0xFFFF9A92),Color(0xFFFFCB77),Color(0xFF82E0B8),Color(0xFF89CDEA),Color(0xFFB6A2ED))
 val trajectory=remember(engine,angle,state.board){engine.trajectory(angle)}
 MiniGameScaffold("泡泡消除",session,"${state.score} 分 · 最佳 ${maxOf(best,state.score)} · 第 ${state.level} 关","点击棋盘瞄准并发射，或拖动调整角度、松开发射。虚线显示墙壁反弹路径。连接三个或更多同色泡泡会消除，失去顶部连接的泡泡也会掉落。连续八次未消除将增加两排。泡泡碰到虚线结束，清空棋盘进入下一关。",{engine=BubbleEngine(level=state.level);state=engine.state;save()}) {
  Text(if(state.won)"全部清空 · 可以进入下一关" else if(state.over)"泡泡到达底线 · 可以重新开始" else "点按发射 · 拖动瞄准 · ${8-state.misses} 次未消除后加行",color=Color(0xFF95AAA5),modifier=Modifier.padding(vertical=4.dp))
  BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
   val width=minOf(maxWidth,maxHeight/1.4f)
   Canvas(Modifier.width(width).height(width*1.4f).background(Color(0xFF101F25)).semantics{contentDescription="泡泡消除棋盘，第 ${state.level} 关，${state.score} 分"}
    .pointerInput(engine,session.paused){detectDragGestures(onDragStart={aim(it,size.width.toFloat())},onDrag={change,_->change.consume();aim(change.position,size.width.toFloat())},onDragEnd={shoot()})}
    .pointerInput(engine,session.paused){detectTapGestures(onTap={aim(it,size.width.toFloat());shoot()})}) {
    val unit=size.width/1000
    fun ball(x:Double,y:Double,color:Int,radius:Float=48f,alpha:Float=1f){val p=Offset(x.toFloat()*unit,y.toFloat()*unit);drawCircle(colors[color].copy(alpha=.15f*alpha),(radius+5)*unit,p);drawCircle(colors[color].copy(alpha=alpha),radius*unit,p);drawCircle(Color.White.copy(alpha=.4f*alpha),radius*.22f*unit,p+Offset(-radius*.28f*unit,-radius*.28f*unit))}
    if(state.projectile==null && !state.over){trajectory.zipWithNext().forEach{(a,b)->drawLine(colors[state.current].copy(alpha=.4f),Offset(a.x.toFloat()*unit,a.y.toFloat()*unit),Offset(b.x.toFloat()*unit,b.y.toFloat()*unit),4*unit,pathEffect=PathEffect.dashPathEffect(floatArrayOf(12*unit,15*unit)))}}
    state.board.forEachIndexed{i,color->if(color>=0){val p=BubbleEngine.center(i/9,i%9);ball(p.x,p.y,color)}}
    if(!reducedMotion && clearNanos!=0L){val progress=((drawNanos-clearNanos)/420_000_000f).coerceIn(0f,1f);if(progress<1)fading.forEach{fade->val p=BubbleEngine.center(fade.row,fade.col);ball(p.x,p.y+if(fade.falling)160*progress*progress else -15*progress,fade.color,48f*(if(fade.falling)1f else 1f-progress*.4f),1f-progress)}}
    drawLine(Color(0xFFDF9988).copy(alpha=.5f),Offset(0f,1160*unit),Offset(size.width,1160*unit),3*unit,pathEffect=PathEffect.dashPathEffect(floatArrayOf(18*unit,14*unit)))
    val shot=state.projectile
    if(shot!=null)ball(shot.x,shot.y,shot.color)else ball(500.0,1300.0,state.current)
    ball(780.0,1300.0,state.next,32f)
    drawCircle(Color(0xFF557179),60*unit,Offset(500*unit,1300*unit),style=androidx.compose.ui.graphics.drawscope.Stroke(3*unit))
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
   MiniAction(if(state.won)"下一关" else if(state.over)"重新开始" else "发射",enabled=!session.paused && state.projectile==null,modifier=Modifier.weight(1f)){
    if(state.over){engine=BubbleEngine(level=if(state.won)(state.level+1).coerceAtMost(999) else state.level);state=engine.state;save()}else shoot()
   }
   Text("右侧小球：下一颗",color=Color(0xFF95AAA5),modifier=Modifier.weight(1f))
  }
 }
}
