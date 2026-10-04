package com.aurora.arcade.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.arcade.GameSession
import com.aurora.arcade.core.*

@Composable fun GameScreen(session: GameSession,onSettings: () -> Unit) {
    val state=session.state
    CompositionLocalProvider(LocalContentColor provides GameText) {
        Column(Modifier.fillMaxSize().padding(horizontal=6.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(bottom=2.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Column(Modifier.width(40.dp).semantics { contentDescription="暂存方块" }
                    .clickable(enabled=!state.holdUsed && !state.gameOver && !session.paused) { session.tap(Action.HOLD) },
                    horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("暂存",color=GameMuted,fontSize=8.sp,lineHeight=10.sp,maxLines=1)
                    PiecePreview(state.held,Modifier.padding(top=2.dp).fillMaxWidth().height(24.dp)
                        .background(GamePanel,RoundedCornerShape(6.dp)).border(1.dp,GameFrame,RoundedCornerShape(6.dp)),if(state.holdUsed) .35f else 1f)
                }
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("本局得分",color=GameMuted,fontSize=8.sp,lineHeight=10.sp,maxLines=1)
                    Text("%,d".format(state.score),fontSize=16.sp,lineHeight=20.sp,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("最高纪录",color=GameMuted,fontSize=8.sp,lineHeight=10.sp,maxLines=1)
                    Text("%,d".format(maxOf(session.best,state.score)),fontSize=12.sp,lineHeight=20.sp,fontFamily=FontFamily.Monospace,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Column(Modifier.width(68.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("接下来",color=GameMuted,fontSize=8.sp,lineHeight=10.sp,maxLines=1)
                    Row(Modifier.padding(top=2.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        state.next.take(2).forEachIndexed { index,kind ->
                            PiecePreview(kind,Modifier.width(32.dp).height(24.dp)
                                .background(GamePanel,RoundedCornerShape(6.dp))
                                .semantics { contentDescription="接下来第${index+1}块 ${kind.name}" })
                        }
                    }
                }
                IconButton(onClick={session.pause()},modifier=Modifier.size(44.dp).semantics { contentDescription="暂停" }) {
                    GameIcon("pause",color=GameText,modifier=Modifier.size(22.dp))
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.TopCenter) {
                // Size the playable cells first; the frame surrounds all 20 complete rows.
                val frame=6.dp
                val cell=minOf((maxHeight-frame)/20f,(maxWidth-frame)/10f).coerceAtLeast(0.dp)
                BoardCanvas(session,Modifier.width(cell*10+frame).height(cell*20+frame).semantics { contentDescription="游戏棋盘" })
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().padding(bottom=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        ControlKey(session,Action.CCW,"ccw","左旋",Modifier.weight(1f).height(68.dp),enabled=!state.gameOver && !session.paused,showLabel=true,iconSize=38.dp)
                        ControlKey(session,Action.CW,"cw","右旋",Modifier.weight(1f).height(68.dp),enabled=!state.gameOver && !session.paused,showLabel=true,iconSize=38.dp)
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(Triple(Action.LEFT,"left","左移"),Triple(Action.SOFT,"down","加速下降"),Triple(Action.RIGHT,"right","右移")).forEach { (action,icon,label) ->
                            ControlKey(session,action,icon,label,Modifier.weight(1f).height(68.dp),enabled=!state.gameOver && !session.paused,iconSize=40.dp)
                        }
                    }
                }
                Column(Modifier.width(56.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    ControlKey(session,Action.UNDO,"undo","撤回到顶部",Modifier.fillMaxWidth().height(54.dp),enabled=session.canUndo && !session.paused,iconSize=26.dp,stackedLabel="回顶部")
                    ControlKey(session,Action.DROP,"drop","快速落底",Modifier.fillMaxWidth().height(54.dp),enabled=!state.gameOver && !session.paused,iconSize=26.dp,stackedLabel="落底")
                }
            }
        }
    }
    if(session.paused) {
        AlertDialog(onDismissRequest={session.resume()},title={Text("歇一会儿")},text={Text("方块正在等你。准备好了，就接着这一局。",color=Muted)},
            confirmButton={TextButton(onClick={session.resume()}) { Text("继续游戏") }},
            dismissButton={Row { TextButton(onClick=onSettings) { Text("设置") };TextButton(onClick={session.home()}) { Text("保存并回首页") } }})
    } else if(state.gameOver) {
        AlertDialog(onDismissRequest={},title={Text("这一局，也很精彩")},text={Column { Text("得分  ${state.score}     消行  ${state.lines}",color=Muted)
            if(session.canUndo) { Spacer(Modifier.height(12.dp));Text("最后一块落错了？还可以撤回到顶部。",color=Mint) } }},
            confirmButton={TextButton(onClick={if(session.canUndo)session.tap(Action.UNDO) else session.newGame(session.engine.mode)}) { Text(if(session.canUndo) "撤回，继续这局" else "再来一局") }},
            dismissButton={TextButton(onClick={session.home()}) { Text("返回首页") }})
    }
}
