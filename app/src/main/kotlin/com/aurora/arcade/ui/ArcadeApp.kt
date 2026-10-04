package com.aurora.arcade.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.arcade.GameSession
import com.aurora.arcade.core.*

@Composable fun ArcadeApp(session: GameSession) {
    var settingsOpen by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    val focus=remember { FocusRequester() }
    LaunchedEffect(session.screen,session.paused,session.state.gameOver) {
        if(session.screen=="game" && !session.paused) {
            focus.requestFocus()
            while(true) {
                val finished = withFrameNanos {
                    session.frame(it)
                    // Finish the last visual effect, then sleep until restart or undo.
                    session.state.gameOver && (session.effect==null || it-session.effectStart>=450_000_000L)
                }
                if(finished) break
            }
        }
    }
    BackHandler(enabled=session.screen=="game") { if(session.paused) session.resume() else session.pause() }
    val background = if(session.screen=="game")
        Brush.horizontalGradient(listOf(GameBackground,GameBackgroundLight,GameBackground))
    else Brush.verticalGradient(listOf(Color(0xFF101D2A),Ink),endY=1000f)
    Box(Modifier.fillMaxSize().background(background)
        .windowInsetsPadding(WindowInsets.safeDrawing).focusRequester(focus)
        .onPreviewKeyEvent { event ->
            val action=when(event.key) {
                Key.DirectionLeft -> Action.LEFT; Key.DirectionRight -> Action.RIGHT; Key.DirectionDown -> Action.SOFT
                Key.DirectionUp,Key.X -> Action.CW; Key.Z -> Action.CCW; Key.Spacebar -> Action.DROP
                Key.C,Key.ShiftLeft -> Action.HOLD; Key.U,Key.Backspace -> Action.UNDO; else -> null
            }
            if(action!=null && session.screen=="game") {
                if(event.type==KeyEventType.KeyDown) session.press(action) else if(event.type==KeyEventType.KeyUp) session.release(action)
                true
            } else if(event.key==Key.Escape && event.type==KeyEventType.KeyUp && session.screen=="game") {
                if(session.paused)session.resume() else session.pause(); true
            } else false
        }.focusable()) {
        if(session.screen=="home") HomeScreen(session,{settingsOpen=true},{helpOpen=true})
        else GameScreen(session) { settingsOpen=true }
        if(settingsOpen) SettingsDialog(session) { settingsOpen=false }
        if(helpOpen) HelpDialog { helpOpen=false }
    }
}

@Composable private fun HomeScreen(session: GameSession,onSettings: ()->Unit,onHelp: ()->Unit) {
    var mode by remember { mutableStateOf(Mode.TIME) }
    var pendingNew by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=14.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(Mint,RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center) {
                Canvas(Modifier.size(23.dp)) {
                    val u=size.width/3
                    listOf(1 to 0,0 to 1,1 to 1,2 to 1).forEach { (x,y) ->drawRoundRect(Ink,Offset(x*u,y*u+u*.5f),androidx.compose.ui.geometry.Size(u-2,u-2),androidx.compose.ui.geometry.CornerRadius(1f)) }
                }
            }
            Column(Modifier.weight(1f).padding(start=12.dp)) {
                Text("微光游乐场",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                Text("A U R O R A   A R C A D E",fontSize=8.sp,color=Muted)
            }
            IconButton(onClick=onSettings,modifier=Modifier.semantics { contentDescription="设置" }) { GameIcon("settings",modifier=Modifier.size(23.dp)) }
        }
        Spacer(Modifier.height(36.dp))
        Text("给自己，一点放松的时间",color=Mint,fontSize=12.sp,letterSpacing=1.sp)
        Spacer(Modifier.height(12.dp))
        Text("让碎片，\n各就其位。",fontSize=36.sp,lineHeight=47.sp,fontWeight=FontWeight.Light,color=Cream,letterSpacing=1.sp)
        Text("熟悉的小游戏，刚刚好的新意。",modifier=Modifier.padding(top=12.dp),color=Muted,fontSize=13.sp)
        Spacer(Modifier.height(28.dp))
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF223B3F),Color(0xFF152332))),RoundedCornerShape(24.dp))
            .border(1.dp,Color(0xFF354D50),RoundedCornerShape(24.dp))) {
            Column(Modifier.padding(22.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("01  /  经典新生",color=Mint,fontSize=11.sp,letterSpacing=1.sp)
                    Spacer(Modifier.weight(1f))
                    Text("离线畅玩",color=Muted,fontSize=10.sp)
                }
                HeroBlocks(Modifier.fillMaxWidth().height(135.dp))
                Text("俄罗斯方块",fontSize=25.sp,fontWeight=FontWeight.SemiBold)
                Text("向下落，向前玩。也可以，重来一步。",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=7.dp))
                Row(Modifier.fillMaxWidth().padding(top=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ModeChip("时光模式",mode==Mode.TIME,Modifier.weight(1f)) { mode=Mode.TIME }
                    ModeChip("经典模式",mode==Mode.CLASSIC,Modifier.weight(1f)) { mode=Mode.CLASSIC }
                }
                Text(if(mode==Mode.TIME) "一键撤回到顶部，放心尝试每一种可能。" else "原汁原味的落块节奏，挑战你的最高分。",
                    color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=10.dp,bottom=16.dp))
                Button(onClick={if(session.hasSaved && !session.state.gameOver)pendingNew=true else session.newGame(mode)},
                    modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Mint,contentColor=Ink)) {
                    Text("开始新的一局",fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f));GameIcon("right",Ink,Modifier.size(22.dp))
                }
                if(session.hasSaved) TextButton(onClick={session.continueGame()},modifier=Modifier.fillMaxWidth().padding(top=6.dp)) {
                    Text("继续上次游戏  ·  ${session.state.score} 分",color=Mint,fontSize=12.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top=20.dp,bottom=16.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("小小游乐场，慢慢长大。",color=Cream,fontSize=12.sp)
                Text("更多小游戏，留给下一次惊喜。",color=Muted,fontSize=10.sp,modifier=Modifier.padding(top=5.dp))
            }
            TextButton(onClick=onHelp) { Text("玩法指南",fontSize=12.sp) }
        }
        Text("为每一次小小的快乐而造  /  01",color=Muted.copy(alpha=.5f),fontSize=9.sp,modifier=Modifier.padding(bottom=22.dp))
    }
    if(pendingNew) AlertDialog(onDismissRequest={pendingNew=false},title={Text("开启新的一局？")},text={Text("当前保存的对局将被新游戏替换。也可以继续上次游戏。")},
        confirmButton={TextButton(onClick={pendingNew=false;session.newGame(mode)}) { Text("开始新游戏") }},
        dismissButton={TextButton(onClick={pendingNew=false;session.continueGame()}) { Text("继续上次") }})
}
@Composable private fun ModeChip(label: String,selected: Boolean,modifier: Modifier,onClick:()->Unit) {
    Box(modifier.height(44.dp).background(if(selected)Mint.copy(alpha=.13f) else Ink.copy(alpha=.25f),RoundedCornerShape(12.dp))
        .border(1.dp,if(selected)Mint.copy(alpha=.55f) else Outline,RoundedCornerShape(12.dp)).clickable(onClick=onClick),contentAlignment=Alignment.Center) {
        Text(label,color=if(selected)Mint else Muted,fontSize=13.sp)
    }
}
@Composable private fun HeroBlocks(modifier: Modifier) {
    Canvas(modifier) {
        val cell=(size.height/4.8f).coerceAtMost(size.width/10)
        val ox=size.width/2-cell*4.5f
        val oy=size.height/2-cell*1.5f
        val layout=listOf(
            Triple(0,2,6),Triple(1,2,6),Triple(2,2,6),Triple(0,1,6),
            Triple(3,1,3),Triple(4,1,3),Triple(5,1,3),Triple(4,0,3),
            Triple(3,2,1),Triple(4,2,1),Triple(5,2,1),Triple(6,2,1),
            Triple(7,1,2),Triple(8,1,2),Triple(7,2,2),Triple(8,2,2))
        layout.forEach { (x,y,k) -> block(Offset(ox+x*cell,oy+y*cell),cell,Kind.entries[k-1],glow=true) }
        drawCircle(Mint.copy(alpha=.7f),2f,Offset(ox+cell*7,oy-cell*.2f))
        drawCircle(Mint.copy(alpha=.3f),1.5f,Offset(ox+cell,oy-cell*.6f))
    }
}
@Composable private fun SettingsDialog(session: GameSession,onDismiss:()->Unit) {
    val s=session.settings
    AlertDialog(onDismissRequest=onDismiss,title={Text("调到你喜欢的手感")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingRow("游戏音效","落底、旋转与消行反馈",s.sound) { session.updateSettings(s.copy(sound=it)) }
        SettingRow("轻触振动","让每次操作有一点回应",s.haptic) { session.updateSettings(s.copy(haptic=it)) }
        SettingRow("灵敏操控","更快的长按连续移动",s.fastControls) { session.updateSettings(s.copy(fastControls=it)) }
        SettingRow("减少动态效果","关闭轨迹、碎片与位移动画",s.reducedMotion) { session.updateSettings(s.copy(reducedMotion=it)) }
    }},confirmButton={TextButton(onClick=onDismiss) { Text("完成") }})
}
@Composable private fun SettingRow(title:String,subtitle:String,checked:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title,fontSize=14.sp);Text(subtitle,color=Muted,fontSize=10.sp) }
        Switch(checked=checked,onCheckedChange=onChange)
    }
}
@Composable private fun HelpDialog(onDismiss:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text("每一块，都有好位置")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("填满横行即可消除，每消除 10 行提升一级。",color=Muted)
        Text("← →  移动，长按可连续移动\n↓  按住加速下降\n旋转键  顺时针 / 逆时针\n快速落底  立即放到底部并锁定\n暂存格  保存当前块，每次落子前限一次",fontSize=13.sp,lineHeight=23.sp)
        Text("时光模式",color=Mint,fontWeight=FontWeight.SemiBold)
        Text("刚刚快速落底的这一块放错了？点击「撤回到顶部」，就能重新摆放。只撤回这一块，不能继续撤回更早的方块；下一块落位后，原来的撤回机会失效。",color=Muted,fontSize=13.sp)
        Text("键盘：方向键移动 / 旋转，空格落底，Z / X 旋转，C 暂存，U 撤回，Esc 暂停。",color=Muted,fontSize=11.sp)
    }},confirmButton={TextButton(onClick=onDismiss) { Text("知道了") }})
}
