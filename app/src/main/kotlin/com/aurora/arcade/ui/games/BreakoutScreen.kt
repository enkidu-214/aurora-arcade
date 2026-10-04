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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aurora.arcade.GameSession
import com.aurora.arcade.MiniGameStore
import com.aurora.arcade.core.games.BreakoutEngine

private data class BrickFade(val index:Int,val time:Long)

@Composable fun BreakoutScreen(session:GameSession,saves:MiniGameStore) {
 var engine by remember {mutableStateOf(BreakoutEngine.restore(saves.load("breakout"))?:BreakoutEngine())}
 var state by remember(engine) {mutableStateOf(engine.state)}
 var trail by remember(engine){mutableStateOf(emptyList<Offset>())}
 var fading by remember(engine){mutableStateOf(emptyList<BrickFade>())}
 var drawNanos by remember(engine){mutableLongStateOf(0L)}
 val reducedMotion=session.settings.reducedMotion
 var best by remember {mutableIntStateOf(saves.best("breakout"))}
 fun save(){saves.save("breakout",engine.save());saves.record("breakout",engine.state.score);best=maxOf(best,engine.state.score)}
 fun move(x:Float,width:Float){if(!session.paused){engine.movePaddle(x/width*1000.0);state=engine.state}}
 fun launch(){if(!session.paused && engine.launch()){state=engine.state;session.miniFeedback();save()}}
 val currentEngine by rememberUpdatedState(engine)
 DisposableEffect(Unit){onDispose{val saved=currentEngine;saves.save("breakout",saved.save());saves.record("breakout",saved.state.score)}}
 LaunchedEffect(session.paused,engine){
  if(session.paused){save();return@LaunchedEffect};var previous=0L;var savedElapsed=0L
  while(!engine.state.over || fading.any{drawNanos-it.time<220_000_000}){withFrameNanos{now->
   drawNanos=now
   if(previous!=0L){val delta=((now-previous)/1_000_000).coerceIn(0,50);val old=engine.state
    if(engine.advance(delta)){
     if(!reducedMotion) {
      trail=(trail+Offset(old.x.toFloat(),old.y.toFloat())).takeLast(9)
      if(engine.state.stage==old.stage) {val hits=old.bricks.indices.filter{old.bricks[it]>0 && engine.state.bricks[it]==0};if(hits.isNotEmpty())fading=(fading.filter{now-it.time<220_000_000}+hits.map{BrickFade(it,now)})}
     };state=engine.state;if(state.score!=old.score || state.lives!=old.lives){session.miniFeedback(state.score!=old.score);save()}}
    savedElapsed+=delta;if(savedElapsed>=2000){save();savedElapsed=0}
   };previous=now
  }}
 }
 MiniGameScaffold("打砖块",session,"${state.score} 分 · 最佳 ${maxOf(best,state.score)} · 第 ${state.stage} 关 · ${state.lives} 次机会","横向拖动棋盘控制挡板，点击开球或轻点棋盘发射。击碎全部砖块进入下一关；球落下失去一次机会。挡板边缘会改变反弹角度。后续关卡会加快球速，并出现需要两次击打的砖块。",{engine=BreakoutEngine();state=engine.state;save()}) {
  Text(if(state.over)"机会用完了 · 可以重新开始" else if(!state.launched)"轻点开球 · 拖动挡板接住小球" else "横向拖动 · 挡板立即跟随",color=Color(0xFF95AAA5),modifier=Modifier.padding(vertical=4.dp))
  BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
   val width=minOf(maxWidth,maxHeight/1.4f)
   Canvas(Modifier.width(width).height(width*1.4f).background(Color(0xFF101F25)).semantics{contentDescription="打砖块棋盘，第 ${state.stage} 关，${state.lives} 次机会"}
    .pointerInput(engine,session.paused){detectDragGestures(onDragStart={move(it.x,size.width.toFloat())},onDrag={change,_->change.consume();move(change.position.x,size.width.toFloat())})}
    .pointerInput(engine,session.paused){detectTapGestures(onTap={move(it.x,size.width.toFloat());launch()})}) {
    val unit=size.width/1000;val palette=listOf(Color(0xFFFFA589),Color(0xFFFFC778),Color(0xFF96DCB6),Color(0xFF80CADC),Color(0xFFACA4EB))
    state.bricks.forEachIndexed{i,hp->if(hp>0){
     val x=BreakoutEngine.brickX(i).toFloat()*unit;val y=BreakoutEngine.brickY(i).toFloat()*unit
     drawRoundRect(palette[i/8],Offset(x,y),Size(100*unit,42*unit),androidx.compose.ui.geometry.CornerRadius(8*unit))
     if(hp==2)drawLine(Color.White.copy(alpha=.65f),Offset(x+30*unit,y+21*unit),Offset(x+70*unit,y+21*unit),4*unit)
    }}
    if(!reducedMotion) {
     fading.forEach{fade->val alpha=(1f-(drawNanos-fade.time)/220_000_000f).coerceIn(0f,1f);if(alpha>0)drawRoundRect(palette[fade.index/8].copy(alpha=alpha*.6f),Offset(BreakoutEngine.brickX(fade.index).toFloat()*unit,BreakoutEngine.brickY(fade.index).toFloat()*unit),Size(100*unit,42*unit),androidx.compose.ui.geometry.CornerRadius(8*unit))}
     if(state.launched)trail.forEachIndexed{i,at->drawCircle(Color(0xFFFFDFB0).copy(alpha=(i+1f)/trail.size*.2f),(6+i*.6f)*unit,at*unit)}
    }
    drawRoundRect(Color(0xFF83E8CF),Offset((state.paddle.toFloat()-100)*unit,1280*unit),Size(200*unit,20*unit),androidx.compose.ui.geometry.CornerRadius(10*unit))
    drawCircle(Color(0xFFFFDFB0).copy(alpha=.15f),24*unit,Offset(state.x.toFloat()*unit,state.y.toFloat()*unit))
    drawCircle(Color(0xFFFFE5BC),14*unit,Offset(state.x.toFloat()*unit,state.y.toFloat()*unit))
    drawLine(Color(0xFF24404A),Offset(0f,1360*unit),Offset(size.width,1360*unit),2*unit)
   }
  }
  MiniAction(if(state.over)"重新开始" else if(state.launched)"拖动棋盘控制挡板" else "开球",enabled=!session.paused && !state.launched,modifier=Modifier.fillMaxWidth()) {
   if(state.over){engine=BreakoutEngine();state=engine.state;save()}else launch()
  }
 }
}
