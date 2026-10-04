package com.aurora.arcade.ui.games

import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aurora.arcade.GameSession
import com.aurora.arcade.MiniGameStore
import com.aurora.arcade.core.games.*
import kotlinx.coroutines.delay

@Composable
fun PinballScreen(session: GameSession, saves: MiniGameStore) {
    var engine by remember { mutableStateOf(PinballEngine.restore(saves.load("pinball")) ?: PinballEngine()) }
    var hud by remember(engine) { mutableStateOf(engine.state) }
    val visuals = remember(engine) { PinballVisuals() }
    var best by remember { mutableIntStateOf(saves.best("pinball")) }
    val reducedMotion by rememberUpdatedState(session.settings.reducedMotion)
    val currentEngine by rememberUpdatedState(engine)
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun save() {
        val latest = engine
        saves.save("pinball", latest.save())
        saves.record("pinball", latest.state.score)
        best = maxOf(best, latest.state.score)
    }
    fun feedback(now: Long) {
        val events = engine.drainEvents()
        events.forEach(session::pinballFeedback)
        visuals.update(now, engine.state, events, reducedMotion)
    }
    fun action() {
        hud = engine.state
        feedback(System.nanoTime())
        save()
    }
    fun restart() {
        engine.clearInputs()
        save()
        engine = PinballEngine()
        hud = engine.state
        save()
    }
    // This effect owns the screen lifetime, rather than the changing engine lifetime.
    DisposableEffect(Unit) {
        onDispose {
            val latest = currentEngine
            latest.clearInputs()
            saves.save("pinball", latest.save())
            saves.record("pinball", latest.state.score)
        }
    }
    DisposableEffect(view, lifecycleOwner) {
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            if (!focused) { currentEngine.clearInputs(); session.pause(); save() }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { currentEngine.clearInputs(); session.pause(); save() }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(engine, session.paused) {
        engine.clearInputs()
        if (session.paused) { hud = engine.state; save(); return@LaunchedEffect }
        var previous = 0L
        var remainderNanos = 0L
        var sinceSave = 0L
        var sinceHud = 0L
        while (true) {
            if (engine.state.gameOver) {
                if (visuals.contacts.isEmpty()) break
            }
            if (engine.state.awaitingLaunch && engine.state.charge == 0.0 && visuals.contacts.isEmpty()) delay(40)
            withFrameNanos { now ->
                val elapsedNanos = if (previous == 0L) 0L else (now - previous).coerceIn(0L, 50_000_000L)
                val accumulatedNanos = elapsedNanos + remainderNanos
                val delta = accumulatedNanos / 1_000_000L
                remainderNanos = accumulatedNanos % 1_000_000L
                previous = now
                val old = engine.state
                engine.advance(delta)
                val events = engine.drainEvents()
                events.forEach(session::pinballFeedback)
                visuals.update(now, engine.state, events, reducedMotion)
                sinceHud += delta
                sinceSave += delta
                val significant = old.lives != engine.state.lives || old.awaitingLaunch != engine.state.awaitingLaunch || old.gameOver != engine.state.gameOver || events.any { it.kind in listOf(PinballEventKind.MISSION, PinballEventKind.MULTIBALL, PinballEventKind.JACKPOT) }
                if (sinceHud >= 100 || significant) { hud = engine.state; sinceHud = 0 }
                if (sinceSave >= 2000 || significant) { save(); sinceSave = 0 }
            }
        }
        hud = engine.state
        save()
    }
    val missionName = when (hud.mission) {
        PinballMission.BUMPERS -> "点亮弹射器"
        PinballMission.TARGETS -> "击中三枚靶标"
        PinballMission.RAMPS -> "飞越坡道与轨道"
        PinballMission.CORE -> "击破反应核心"
    }
    MiniGameScaffold(
        "超新星弹球", session,
        "${pinballScore(hud.score)} 分 · 最高 ${pinballScore(maxOf(best, hud.score))} · 球 ${hud.lives}",
        "按住中央按钮蓄力，松手开球；青色区松手有技巧奖励。两侧挡板按下立即抬起，可同时用两个手指操作。\n\n跟随任务依次击中弹射器、三枚不同靶标、坡道或轨道和反应核心。命中靶标和顶端球道增加分数倍率；连续击球累积连击。\n\n能量满后按中央「超新星」：三球同时在场，20 秒双倍得分，坡道和核心触发大奖。开球和超新星有 8 秒救球。\n\n「震台」可向上救球，冷却 3 秒。三次机会用完后挑战更高记录。暂停或离开会保存当前球桌。",
        ::restart,
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 5.dp)) {
                Text(if (hud.gameOver) "本轮结束 · ${pinballScore(hud.score)} 分" else "${hud.sector.toString().padStart(2, '0')}  /  $missionName", color = Color(0xFFF3D096), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(if (hud.gameOver) "最高纪录 ${pinballScore(maxOf(best, hud.score))}" else "进度 ${hud.missionProgress}/${hud.missionGoal}  ·  ×${hud.multiplier} 倍率${if (hud.combo > 1) "  ·  ${hud.combo} 连击" else ""}", color = Color(0xFF9AB1C2), fontSize = 11.sp, maxLines = 1)
            }
            TextButton(onClick = { if (engine.nudge()) action() }, enabled = !session.paused && !hud.gameOver && !hud.awaitingLaunch && hud.nudgeCooldownMs == 0L, modifier = Modifier.widthIn(min = 56.dp).heightIn(min = 48.dp).semantics { contentDescription = "震台" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(if (hud.nudgeCooldownMs > 0) "${(hud.nudgeCooldownMs + 999) / 1000}s" else "震台 ↑", color = if (hud.nudgeCooldownMs > 0) Color(0xFF698598) else Color(0xFF79D5DE), fontSize = 12.sp)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0B1420), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            PinballCanvas(engine, visuals, reducedMotion, Modifier.fillMaxSize().semantics {
                contentDescription = "超新星球桌，${hud.balls.size} 颗球，$missionName，进度 ${hud.missionProgress}/${hud.missionGoal}"
            })
        }
        Spacer(Modifier.height(6.dp))
        PinballControls(engine, hud, !session.paused, ::action, ::restart)
    }
}

private fun pinballScore(value: Int): String = value.toString().reversed().chunked(3).joinToString(",").reversed()
