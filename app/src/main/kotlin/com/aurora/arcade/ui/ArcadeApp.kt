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
import com.aurora.arcade.MiniGameStore
import com.aurora.arcade.ui.games.*
import androidx.compose.ui.platform.LocalContext
import com.aurora.arcade.core.*

@Composable fun ArcadeApp(session: GameSession) {
    val context = LocalContext.current
    val saves = remember(context) { MiniGameStore(context) }
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
    val background = if(session.screen!="home")
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
        when(session.screen) {
            "home" -> CollectionHome(session, saves) { settingsOpen=true }
            "mini:pinball" -> PinballScreen(session, saves)
            "mini:2048" -> Twenty48Screen(session, saves)
            "mini:snake" -> SnakeScreen(session, saves)
            "mini:breakout" -> BreakoutScreen(session, saves)
            "mini:minesweeper" -> MinesweeperScreen(session, saves)
            "mini:sokoban" -> SokobanScreen(session, saves)
            "mini:link" -> LinkPairsScreen(session, saves)
            "mini:bubble" -> BubbleScreen(session, saves)
            "mini:spider" -> SpiderScreen(session, saves)
            else -> GameScreen(session) { settingsOpen=true }
        }
        if(settingsOpen) SettingsDialog(session) { settingsOpen=false }
        if(helpOpen) HelpDialog { helpOpen=false }
    }
}

@Composable private fun SettingsDialog(session: GameSession,onDismiss:()->Unit) {
    val s=session.settings
    AlertDialog(onDismissRequest=onDismiss,title={Text("调到你喜欢的手感")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingRow("游戏音效","操作与消除的轻快反馈",s.sound) { session.updateSettings(s.copy(sound=it)) }
        SettingRow("轻触振动","让每次操作有一点回应",s.haptic) { session.updateSettings(s.copy(haptic=it)) }
        SettingRow("灵敏操控","俄罗斯方块的长按连续移动",s.fastControls) { session.updateSettings(s.copy(fastControls=it)) }
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
