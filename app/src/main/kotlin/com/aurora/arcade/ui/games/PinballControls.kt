package com.aurora.arcade.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.arcade.core.games.PinballEngine
import com.aurora.arcade.core.games.PinballState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One pointer dispatcher keeps both flippers independent, including ACTION_CANCEL. */
@Composable
internal fun PinballControls(
    engine: PinballEngine,
    state: PinballState,
    enabled: Boolean,
    onAction: () -> Unit,
    onRestart: () -> Unit,
) {
    var leftHeld by remember(engine) { mutableStateOf(false) }
    var rightHeld by remember(engine) { mutableStateOf(false) }
    var centerHeld by remember(engine) { mutableStateOf(false) }
    val latestAction by rememberUpdatedState(onAction)
    val latestRestart by rememberUpdatedState(onRestart)
    val scope = rememberCoroutineScope()
    val ready = state.energy >= 100 && state.novaRemainingMs == 0L
    val centerLabel = when {
        state.gameOver -> "再来一局"
        state.awaitingLaunch -> if (centerHeld) "松手发射" else "按住蓄力"
        ready -> "超新星"
        state.novaRemainingMs > 0L -> "三球爆发"
        else -> "能量 ${state.energy}%"
    }
    Row(
        Modifier.fillMaxWidth().height(78.dp).pointerInput(engine, enabled) {
            val pointers = mutableMapOf<Long, Int>()
            var charging = false
            fun applyFlippers() {
                leftHeld = enabled && pointers.values.any { it == 0 }
                rightHeld = enabled && pointers.values.any { it == 2 }
                engine.setFlippers(leftHeld, rightHeld)
            }
            try {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            val id = change.id.value
                            if (change.pressed && !change.previousPressed && enabled) {
                                val region = when {
                                    change.position.x < size.width * .325f -> 0
                                    change.position.x > size.width * .675f -> 2
                                    else -> 1
                                }
                                pointers[id] = region
                                if (region == 1 && !centerHeld) {
                                    centerHeld = true
                                    when {
                                        engine.state.gameOver -> latestRestart()
                                        engine.state.awaitingLaunch -> { engine.startCharge(); charging = true }
                                        engine.state.energy >= 100 -> engine.activateNova()
                                    }
                                    latestAction()
                                }
                            }
                            if (!change.pressed && change.previousPressed) {
                                val region = pointers.remove(id)
                                if (region == 1 && pointers.values.none { it == 1 }) {
                                    centerHeld = false
                                    if (charging) { charging = false; engine.releaseCharge(); latestAction() }
                                }
                            }
                            if (change.pressed || change.previousPressed) change.consume()
                        }
                        applyFlippers()
                    }
                }
            } finally {
                pointers.clear()
                leftHeld = false; rightHeld = false; centerHeld = false
                engine.clearInputs()
            }
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ControlFace("左挡板", "L  ↗", leftHeld, enabled, Color(0xFF75E9ED), Modifier.weight(.325f)) {
            if (enabled) scope.launch { engine.setFlippers(true, rightHeld); leftHeld = true; delay(120); leftHeld = false; engine.setFlippers(false, rightHeld) }
        }
        ControlFace(centerLabel, when {
            state.awaitingLaunch -> "${(state.charge * 100).toInt()}%"
            ready -> "NOVA × 3"
            state.gameOver -> "重新开始"
            state.novaRemainingMs > 0L -> "${(state.novaRemainingMs + 999) / 1000}s · 双倍"
            else -> "击球充能"
        }, centerHeld || ready, enabled, Color(0xFFFFCB77), Modifier.weight(.35f), semanticHeld = centerHeld) {
            if (enabled) {
                when {
                    engine.state.gameOver -> latestRestart()
                    engine.state.awaitingLaunch -> { engine.startCharge(); engine.releaseCharge() }
                    engine.state.energy >= 100 -> engine.activateNova()
                }
                latestAction()
            }
        }
        ControlFace("右挡板", "↖  R", rightHeld, enabled, Color(0xFF75E9ED), Modifier.weight(.325f)) {
            if (enabled) scope.launch { engine.setFlippers(leftHeld, true); rightHeld = true; delay(120); rightHeld = false; engine.setFlippers(leftHeld, false) }
        }
    }
}

@Composable
private fun ControlFace(label: String, detail: String, held: Boolean, enabled: Boolean, accent: Color, modifier: Modifier, semanticHeld: Boolean = held, activate: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxHeight().background(
            Brush.verticalGradient(if (held && enabled) listOf(accent.copy(alpha = .3f), Color(0xFF182D39)) else listOf(Color(0xFF233744), Color(0xFF14212F))), shape,
        ).border(if (held && enabled) 2.dp else 1.dp, accent.copy(alpha = if (enabled && held) .9f else .3f), shape)
            .semantics(mergeDescendants = true) { role = Role.Button; contentDescription = if (label == "按住蓄力" || label == "松手发射") "蓄力发射" else label; stateDescription = if (semanticHeld) "按下" else "松开"; onClick { activate(); true } }
            .padding(horizontal = 3.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = if (enabled) accent else Color(0xFF778995), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(detail, color = Color(0xFFB4C9D7), fontSize = 10.sp, maxLines = 1)
    }
}
