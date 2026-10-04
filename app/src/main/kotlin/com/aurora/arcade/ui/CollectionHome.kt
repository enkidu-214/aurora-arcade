package com.aurora.arcade.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.arcade.GameSession
import com.aurora.arcade.MiniGameStore
import com.aurora.arcade.core.Mode

private data class GameEntry(val id: String, val title: String, val detail: String, val color: Color)
private val entries = listOf(
    GameEntry("pinball", "超新星弹球", "三球爆发 · 核心首领", Color(0xFF75DDEA)),
    GameEntry("2048", "2048", "滑动合并 · 再想一步", Color(0xFFFFC978)),
    GameEntry("snake", "贪吃蛇", "指尖转弯 · 越吃越长", Color(0xFF9CE4B0)),
    GameEntry("breakout", "打砖块", "跟手挡板 · 弹出连击", Color(0xFF87C8F2)),
    GameEntry("minesweeper", "扫雷", "步步推理 · 安全开局", Color(0xFFEBAD9A)),
    GameEntry("sokoban", "推箱子", "小小仓库 · 慢慢琢磨", Color(0xFFE0C796)),
    GameEntry("link", "连连看", "相同相连 · 轻松消除", Color(0xFFC5B0F0)),
    GameEntry("bubble", "泡泡消除", "瞄准反弹 · 一串落下", Color(0xFFF5A5C2)),
    GameEntry("spider", "蜘蛛纸牌", "单花色 · 从 K 到 A", Color(0xFF9FD8C9)),
)

@Composable
fun CollectionHome(session: GameSession, saves: MiniGameStore, onSettings: () -> Unit) {
    var tetrisOpen by remember { mutableStateOf(false) }
    var replaceOpen by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(Mode.TIME) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("微光游乐场", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Text("A U R O R A  A R C A D E", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 5.dp))
            }
            IconButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = "设置" }) {
                GameIcon("settings", modifier = Modifier.size(24.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("今天，想玩点什么？", color = GameText, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("10 款 · 离线畅玩", color = Mint, fontSize = 11.sp)
        }
        Box(Modifier.fillMaxWidth().background(
            Brush.horizontalGradient(listOf(Color(0xFF263F42), Color(0xFF20323E))), RoundedCornerShape(21.dp))
            .border(1.dp, Color(0xFF45605F), RoundedCornerShape(21.dp)).clickable { tetrisOpen = true }
            .padding(20.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Text("经典新生  /  01", color = Mint, fontSize = 10.sp)
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("俄罗斯方块", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                        Text("熟悉的节奏，多一次重新摆放。", fontSize = 11.sp, color = GameMuted, modifier = Modifier.padding(top = 7.dp))
                    }
                    Canvas(Modifier.size(66.dp)) {
                        val u = size.width / 3
                        listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1).forEach { (x, y) ->
                            block(Offset(x*u, y*u+u*.5f), u, com.aurora.arcade.core.Kind.T)
                        }
                    }
                }
                Text(if (session.hasSaved) "继续 / 开始新的一局  →" else "开始游戏  →",
                    color = Mint, fontSize = 12.sp, modifier = Modifier.padding(top = 17.dp))
            }
        }
        Text("换一种小快乐", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 23.dp, bottom = 12.dp))
        entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { entry ->
                    val saved = saves.load(entry.id) != null
                    Column(Modifier.weight(1f).fillMaxHeight().background(Panel, RoundedCornerShape(18.dp))
                        .border(1.dp, Outline, RoundedCornerShape(18.dp))
                        .clickable { session.openMiniGame(entry.id) }.padding(15.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            CollectionIcon(entry.id, entry.color, Modifier.size(34.dp))
                            Spacer(Modifier.weight(1f))
                            Text(if (saved) "继续" else "开始", color = entry.color.copy(alpha = .85f), fontSize = 10.sp)
                        }
                        Text(entry.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                        Text(entry.detail, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
        Text("进度自动保存，随时回来接着玩。", color = Muted, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 7.dp, bottom = 24.dp))
    }
    if (tetrisOpen) AlertDialog(onDismissRequest = { tetrisOpen = false }, title = { Text("俄罗斯方块") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("填满横行即可消除，每消除 10 行提升一级。", color = GameMuted)
            listOf(Mode.TIME to "时光模式 · 一键回顶部", Mode.CLASSIC to "经典模式 · 挑战最高分").forEach { (choice, label) ->
                Row(Modifier.fillMaxWidth().clickable { mode = choice }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == choice, onClick = { mode = choice })
                    Text(label, fontSize = 13.sp)
                }
            }
            Text("大按键移动 / 旋转，侧边快速落底与回顶部。顶部暂存格可以交换方块。", color = Muted, fontSize = 12.sp)
        }
    }, confirmButton = { TextButton(onClick = {
        tetrisOpen = false
        if (session.hasSaved && !session.state.gameOver) replaceOpen = true else session.newGame(mode)
    }) { Text("开始新游戏") } }, dismissButton = {
        if (session.hasSaved) TextButton(onClick = { tetrisOpen = false; session.continueGame() }) { Text("继续上次") }
    })
    if (replaceOpen) AlertDialog(onDismissRequest = { replaceOpen = false }, title = { Text("替换当前俄罗斯方块对局？") },
        text = { Text("已有对局仍可继续，也可以开始新的一局。") },
        confirmButton = { TextButton(onClick = { replaceOpen = false; session.newGame(mode) }) { Text("开始新游戏") } },
        dismissButton = { TextButton(onClick = { replaceOpen = false; session.continueGame() }) { Text("继续上次") } })
}

@Composable
private fun CollectionIcon(id: String, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val u = size.width / 10
        fun rect(x: Float, y: Float, w: Float, h: Float) = drawRoundRect(color, Offset(x*u, y*u), Size(w*u, h*u), CornerRadius(u*.5f))
        when (id) {
            "pinball" -> {
                drawRoundRect(color.copy(alpha=.35f),Offset(u*.5f,u*.3f),Size(9*u,9*u),CornerRadius(2*u),style=Stroke(.55f*u))
                drawCircle(color,1.6f*u,Offset(5*u,3.2f*u))
                drawCircle(Color.White.copy(alpha=.7f),.45f*u,Offset(4.5f*u,2.7f*u))
                drawLine(color,Offset(2*u,7.2f*u),Offset(4.2f*u,8.3f*u),u,androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color,Offset(8*u,7.2f*u),Offset(5.8f*u,8.3f*u),u,androidx.compose.ui.graphics.StrokeCap.Round)
            }
            "2048" -> { rect(0f, 0f, 4.4f, 4.4f); rect(5.5f, 5.5f, 4.4f, 4.4f); rect(0f, 5.5f, 4.4f, 4.4f) }
            "snake" -> { rect(0f, 1f, 8f, 2.6f); rect(5.4f, 1f, 2.6f, 8f); rect(1f, 6.4f, 7f, 2.6f); drawCircle(color, u, Offset(9*u, u*9)) }
            "breakout" -> { for (x in 0..2) for(y in 0..1) rect(x*3.5f,y*2.7f,2.7f,1.9f); rect(2f,8f,6f,1.3f);drawCircle(color,u*.8f,Offset(5*u,6.3f*u)) }
            "minesweeper" -> { drawLine(color,Offset(2*u,9*u),Offset(2*u,u),u*.6f); drawPath(Path().apply { moveTo(2*u,u);lineTo(9*u,3.3f*u);lineTo(2*u,5.5f*u);close() },color);rect(0f,9f,5f,1f) }
            "sokoban" -> { drawRoundRect(color,Offset(u,u),Size(8*u,8*u),CornerRadius(u),style=Stroke(u*.7f));drawLine(color,Offset(3*u,3*u),Offset(7*u,7*u),u*.7f);drawLine(color,Offset(3*u,7*u),Offset(7*u,3*u),u*.7f) }
            "link" -> { drawCircle(color,2.3f*u,Offset(2.6f*u,3*u),style=Stroke(.7f*u));drawCircle(color,2.3f*u,Offset(7.4f*u,7*u),style=Stroke(.7f*u));drawLine(color,Offset(3.8f*u,4*u),Offset(6.2f*u,6*u),u*.7f) }
            "bubble" -> { drawCircle(color,2.4f*u,Offset(2.6f*u,3*u));drawCircle(color.copy(alpha=.7f),2.4f*u,Offset(7.4f*u,3*u));drawCircle(color.copy(alpha=.85f),2.4f*u,Offset(5*u,7.2f*u)) }
            "spider" -> { drawRoundRect(color.copy(alpha=.4f),Offset(u,0f),Size(6*u,8*u),CornerRadius(u));drawRoundRect(color,Offset(3*u,2*u),Size(6*u,8*u),CornerRadius(u));drawPath(Path().apply { moveTo(6*u,3.5f*u);lineTo(8*u,6*u);lineTo(6*u,8.5f*u);lineTo(4*u,6*u);close() },Panel) }
        }
    }
}
