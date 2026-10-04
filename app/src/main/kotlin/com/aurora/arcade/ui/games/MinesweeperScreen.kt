package com.aurora.arcade.ui.games

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.arcade.GameSession
import com.aurora.arcade.MiniGameStore
import com.aurora.arcade.core.games.*
import com.aurora.arcade.ui.*

@Composable
fun MinesweeperScreen(session:GameSession,saves:MiniGameStore) {
    var engine by remember {mutableStateOf(MinesweeperEngine.restore(saves.load("minesweeper")))}
    var state by remember {mutableStateOf(engine.state)}
    var flagsMode by remember {mutableStateOf(false)}
    val currentEngine by rememberUpdatedState(engine)
    fun persist() {saves.save("minesweeper",engine.save());saves.record("minesweeper",state.cleared)}
    fun refresh() {state=engine.state;persist()}
    fun restart() {engine=MinesweeperEngine(state.difficulty);refresh()}
    fun act(index:Int,flag:Boolean) {
        if(!session.paused && (if(flag)engine.flag(index) else engine.open(index))) {refresh();session.miniFeedback(state.won)}
    }
    DisposableEffect(Unit) {onDispose {saves.save("minesweeper",currentEngine.save())}}
    LaunchedEffect(session.paused) {if(session.paused)persist()}
    MiniGameScaffold("扫雷",session,"${if(state.difficulty==MineDifficulty.EASY)"初级" else "中级"} · 剩余雷数 ${state.remaining} · 已开 ${state.cleared}",
        "第一次开格及周围八格一定安全。数字表示周围八格的雷数。点格子开格，长按插旗；也可以切换下方开格／插旗模式。再次点已打开的数字，周围旗数与数字相等时会同时打开其余邻格，错误的旗也会导致踩雷。打开全部无雷格就获胜。初级 8×8、10 雷；中级 10×12、20 雷。",::restart) {
        Canvas(Modifier.weight(1f).fillMaxWidth().semantics {contentDescription="扫雷棋盘，点击开格，长按插旗"}
            .pointerInput(state.difficulty,session.paused,flagsMode) {
                fun index(point:Offset):Int {
                    val cell=minOf(size.width.toFloat()/state.width,size.height.toFloat()/state.height)
                    val ox=(size.width-cell*state.width)/2f;val oy=(size.height-cell*state.height)/2f
                    if(point.x<ox || point.y<oy || point.x>=ox+cell*state.width || point.y>=oy+cell*state.height)return -1
                    return ((point.y-oy)/cell).toInt()*state.width+((point.x-ox)/cell).toInt()
                }
                detectTapGestures(onTap={act(index(it),flagsMode)},onLongPress={act(index(it),true)})
            }) {
            val cell=minOf(size.width/state.width,size.height/state.height)
            val origin=Offset((size.width-cell*state.width)/2f,(size.height-cell*state.height)/2f)
            val gap=2.dp.toPx();val radius=CornerRadius(5.dp.toPx())
            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {textAlign=Paint.Align.CENTER;typeface=Typeface.create("sans-serif",Typeface.BOLD);textSize=cell*.52f}
            val numberColors=listOf(Mint,Color(0xFF91C5FA),Color(0xFFA9DFA8),Color(0xFFF2A9A7),Color(0xFFCEB0EF),Color(0xFFE7BA83),Color(0xFF92DFDE),Cream,Cream)
            repeat(state.width*state.height) {i->
                val p=origin+Offset(i%state.width*cell,i/state.width*cell);val center=p+Offset(cell/2,cell/2)
                val revealed=state.opened[i];val mineVisible=state.mines[i] && (state.lost || state.won)
                drawRoundRect(if(i==state.exploded)Color(0xFF9B4555) else if(revealed)GamePanel else GameFrame,p+Offset(gap/2,gap/2),Size(cell-gap,cell-gap),radius)
                if(mineVisible) {
                    drawCircle(if(state.won)Mint else Color(0xFFE7A5AD),cell*.16f,center)
                    repeat(4) {n->val dx=if(n==0 || n==2)cell*.25f else 0f;val dy=if(n==1 || n==2)cell*.25f else if(n==3)-cell*.25f else 0f
                        drawLine(if(state.won)Mint else Color(0xFFE7A5AD),center-Offset(dx,dy),center+Offset(dx,dy),cell*.06f)}
                } else if(state.flags[i]) {
                    drawLine(Cream,p+Offset(cell*.38f,cell*.25f),p+Offset(cell*.38f,cell*.77f),cell*.05f)
                    val flag=Path().apply{moveTo(p.x+cell*.4f,p.y+cell*.24f);lineTo(p.x+cell*.74f,p.y+cell*.38f);lineTo(p.x+cell*.4f,p.y+cell*.5f);close()}
                    drawPath(flag,if(state.lost && !state.mines[i])Color(0xFFF19FA7) else Mint)
                } else if(revealed) {
                    val number=state.adjacent(i);if(number>0) {paint.color=numberColors[number].toArgb();drawContext.canvas.nativeCanvas.drawText(number.toString(),center.x,center.y-(paint.ascent()+paint.descent())/2,paint)}
                } else drawCircle(GameFrameLight.copy(alpha=.45f),cell*.04f,center)
            }
        }
        Text(when {state.won->"全部安全格已打开 · 扫雷成功！";state.lost->"踩到雷了 · 新一局继续挑战";flagsMode->"插旗模式 · 点格子插旗／取消";else->"开格模式 · 长按格子也可以插旗"},color=if(state.won || state.lost)Mint else GameMuted,fontSize=12.sp,modifier=Modifier.padding(vertical=6.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MiniAction(if(flagsMode)"当前：插旗" else "当前：开格",!session.paused,Modifier.weight(1f)) {if(!session.paused)flagsMode=!flagsMode}
            MiniAction("新一局",!session.paused,Modifier.weight(1f)) {if(!session.paused){if(state.won || state.lost || !state.generated)restart() else session.pause()}}
        }
        Row(Modifier.fillMaxWidth().padding(top=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MineDifficulty.entries.forEach {difficulty->MiniAction(if(difficulty==MineDifficulty.EASY)"初级 · 10 雷" else "中级 · 20 雷",!session.paused && state.difficulty!=difficulty,Modifier.weight(1f)) {
                if(!session.paused){engine=MinesweeperEngine(difficulty);refresh()}
            }}
        }
    }
}
