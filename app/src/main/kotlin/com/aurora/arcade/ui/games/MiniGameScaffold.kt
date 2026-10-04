package com.aurora.arcade.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.arcade.GameSession
import com.aurora.arcade.ui.*

@Composable
fun MiniGameScaffold(
    title: String,
    session: GameSession,
    status: String,
    help: String,
    onRestart: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var helpOpen by remember { mutableStateOf(false) }
    var restartOpen by remember { mutableStateOf(false) }
    BackHandler {
        if (helpOpen) helpOpen = false
        else if (restartOpen) restartOpen = false
        else if (session.paused) session.resume() else session.pause()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                color = GameText, modifier = Modifier.weight(1f).padding(start = 4.dp))
            TextButton(onClick = { session.pause(); helpOpen = true }) { Text("玩法", color = GameMuted) }
            TextButton(onClick = { session.pause() }) { Text("暂停", color = Mint) }
        }
        Text(status, color = GameMuted, fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().background(GamePanel, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
        Column(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), content = content)
    }
    if (helpOpen) AlertDialog(
        onDismissRequest = { helpOpen = false }, title = { Text(title) },
        text = { Text(help, modifier = Modifier.verticalScroll(rememberScrollState()), lineHeight = 24.sp) },
        confirmButton = { TextButton(onClick = { helpOpen = false; session.resume() }) { Text("开始玩") } },
    ) else if (restartOpen) AlertDialog(
        onDismissRequest = { restartOpen = false }, title = { Text("重新开始？") },
        text = { Text("这一局的进度将被替换，已获得的最高记录会保留。") },
        confirmButton = { TextButton(onClick = { restartOpen = false; onRestart(); session.resume() }) { Text("重新开始") } },
        dismissButton = { TextButton(onClick = { restartOpen = false }) { Text("取消") } },
    ) else if (session.paused) AlertDialog(
        onDismissRequest = { session.resume() }, title = { Text("休息一下") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$title · 进度已保存", color = GameMuted)
                TextButton(onClick = { restartOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("重新开始") }
                TextButton(onClick = { session.home() }, modifier = Modifier.fillMaxWidth()) { Text("返回游乐场") }
            }
        },
        confirmButton = { TextButton(onClick = { session.resume() }) { Text("继续游戏") } },
    )
}

@Composable
fun MiniAction(label: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GameFrame, contentColor = GameText)) {
        Text(label, fontSize = if (label in listOf("←", "↑", "↓", "→")) 28.sp else 14.sp, fontWeight = FontWeight.Medium)
    }
}
