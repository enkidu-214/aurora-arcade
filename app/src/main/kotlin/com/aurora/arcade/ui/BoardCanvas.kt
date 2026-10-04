package com.aurora.arcade.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import com.aurora.arcade.GameSession
import com.aurora.arcade.core.*
import kotlin.math.*

private class Motion {
    var id = -1L; var x = 0f; var y = 0f; var time = 0L
}

@Composable fun BoardCanvas(session: GameSession,modifier: Modifier = Modifier) {
    val state = session.state
    val effect = session.effect
    val start = session.effectStart
    val reduced = session.settings.reducedMotion
    val motion = remember { Motion() }
    val ghost = remember(state.active,state.board) { session.engine.landing() }
    Canvas(modifier.background(Color.Black,RoundedCornerShape(5.dp))
        .border(3.dp,Brush.linearGradient(listOf(GameFrame,GameFrameLight,GameFrame)),RoundedCornerShape(5.dp)).padding(3.dp)) {
        // Read the clock during drawing so idle animation frames don't recompose the board.
        val frame = session.frameNanos
        val cell = size.width/10f
        val age = ((frame-start)/1_000_000f).coerceAtLeast(0f)
        val dt = ((frame-motion.time)/1_000_000f).coerceIn(0f,50f)
        motion.time = frame
        val active = state.active
        if(motion.id != state.pieceId || reduced) {
            motion.id = state.pieceId; motion.x = active.x.toFloat(); motion.y = active.y.toFloat()
        } else {
            motion.x += (active.x-motion.x)*(1f-exp(-dt/18f))
            motion.y += (active.y-motion.y)*(1f-exp(-dt/24f))
        }
        clipRect {
            for(x in 1..9) drawLine(GameGrid,Offset(x*cell,0f),Offset(x*cell,size.height),1f)
            for(y in 1..19) drawLine(GameGrid,Offset(0f,y*cell),Offset(size.width,y*cell),1f)
            // Collapse survivors from their original row over 180ms, independently of game state.
            val clearing = !reduced && effect?.type == EventType.CLEAR && age < 180f
            if(clearing) {
                val progress = (age/180f).coerceIn(0f,1f)
                val collapse = 1f-(1f-progress).pow(3)
                for(y in HIDDEN_ROWS until ROWS) for(x in 0..9) {
                    val color = effect.lockedBoard[y*10+x]
                    if(color == 0) continue
                    if(y in effect.rows) {
                        block(Offset(x*cell,(y-HIDDEN_ROWS)*cell),cell,Kind.entries[color-1],1f-progress)
                        drawRect(Cream.copy(alpha=(1f-progress)*.85f),Offset(x*cell,(y-HIDDEN_ROWS)*cell),Size(cell,cell))
                    } else {
                        val fall = effect.rows.count { it>y } * collapse
                        block(Offset(x*cell,(y-HIDDEN_ROWS+fall)*cell),cell,Kind.entries[color-1])
                    }
                }
            } else {
                for(y in HIDDEN_ROWS until ROWS) for(x in 0..9) {
                    val color = state.board[y*10+x]
                    if(color != 0) block(Offset(x*cell,(y-HIDDEN_ROWS)*cell),cell,Kind.entries[color-1])
                }
            }
            if(!state.gameOver) {
                Pieces.cells(ghost).forEach { p ->
                    val pos = Offset(p.x*cell+2f,(p.y-HIDDEN_ROWS)*cell+2f)
                    drawRoundRect(PieceColors[active.kind.ordinal+1].copy(alpha=.07f),pos,Size(cell-4f,cell-4f),CornerRadius(cell*.035f))
                    drawRoundRect(PieceColors[active.kind.ordinal+1].copy(alpha=.4f),pos,Size(cell-4f,cell-4f),CornerRadius(cell*.035f),style=Stroke(1.2f))
                }
                val undoing = !reduced && effect?.type == EventType.UNDO && age<230f && active == effect.to
                val drawX = if(undoing) {
                    val t=1f-(1f-age/230f).pow(4); effect!!.from.x+(active.x-effect.from.x)*t
                } else motion.x
                val drawY = if(undoing) {
                    val t=1f-(1f-age/230f).pow(4); effect!!.from.y+(active.y-effect.from.y)*t
                } else motion.y
                val rotating = !reduced && effect?.type == EventType.ROTATE && age<85f && effect.to.rotation == active.rotation
                if(rotating && active.kind != Kind.O) {
                    val direction = if((effect.to.rotation-effect.from.rotation+4)%4==1) 1 else -1
                    val pivot = if(active.kind==Kind.I) 2f else 1.5f
                    withTransform({rotate(direction*90f*(1f-(1f-age/85f).pow(3)),Offset((drawX+pivot)*cell,(drawY-HIDDEN_ROWS+pivot)*cell))}) {
                        Pieces.cells(active.kind,effect.from.rotation).forEach { p -> block(Offset((drawX+p.x)*cell,(drawY+p.y-HIDDEN_ROWS)*cell),cell,active.kind,glow=true) }
                    }
                } else {
                    Pieces.cells(active.kind,active.rotation).forEach { p -> block(Offset((drawX+p.x)*cell,(drawY+p.y-HIDDEN_ROWS)*cell),cell,active.kind,glow=true) }
                }
            }
            if(!reduced && effect != null) drawEffect(effect,age,cell)
            // Quiet ceiling markers, visible even on a nearly empty board.
            drawLine(GameFrameLight.copy(alpha=.4f),Offset(0f,1f),Offset(cell*.65f,1f),2f)
            drawLine(GameFrameLight.copy(alpha=.4f),Offset(size.width-cell*.65f,1f),Offset(size.width,1f),2f)
        }
    }
}

private fun DrawScope.drawEffect(effect: GameEvent,age: Float,cell: Float) {
    val color = PieceColors[effect.to.kind.ordinal+1]
    if(effect.type in listOf(EventType.DROP,EventType.CLEAR) && age<190f) {
        val alpha = (1f-age/190f).coerceIn(0f,1f)
        Pieces.cells(effect.to.kind,effect.to.rotation).forEach { p ->
            val x=(effect.to.x+p.x)*cell
            val y=(effect.to.y+p.y-HIDDEN_ROWS)*cell
            val fromY=(effect.from.y+p.y-HIDDEN_ROWS)*cell
            if(y>fromY) drawRect(Brush.verticalGradient(listOf(Color.Transparent,color.copy(alpha=.28f*alpha)),fromY,y+cell),Offset(x+cell*.12f,fromY),Size(cell*.76f,y-fromY+cell))
            drawRoundRect(Cream.copy(alpha=alpha*.6f),Offset(x+1,y+1),Size(cell-2,cell-2),CornerRadius(cell*.035f),style=Stroke(2f))
        }
    }
    if(effect.type == EventType.CLEAR && age<430f) {
        val t = age/430f
        effect.rows.filter { it>=HIDDEN_ROWS }.forEach { row ->
            for(i in 0..17) {
                val origin = (i+.5f)/18f*size.width
                val vx = sin(i*7.3f)*cell*1.8f
                val vy = cos(i*3.7f)*cell*2.8f
                val point = Offset(origin+vx*t,(row-HIDDEN_ROWS+.5f)*cell+vy*t+cell*t*t)
                drawCircle(if(i%3==0) Cream else Mint,(cell*.065f*(1-t)).coerceAtLeast(.1f),point,(1-t)*.8f)
            }
        }
    }
    if(effect.type == EventType.UNDO && age<330f) {
        val alpha = (1f-age/330f)*.2f
        val x=(effect.from.x+1.5f)*cell
        drawRect(Brush.verticalGradient(listOf(Mint.copy(alpha=alpha),Color.Transparent)),Offset((x-cell*2).coerceAtLeast(0f),0f),Size(cell*4,size.height))
    }
}

@Composable fun PiecePreview(kind: Kind?,modifier: Modifier=Modifier,alpha: Float=1f) {
    Canvas(modifier) {
        if(kind == null) {
            drawLine(Muted.copy(alpha=.35f),Offset(size.width*.35f,size.height*.5f),Offset(size.width*.65f,size.height*.5f),2f,StrokeCap.Round)
        } else {
            val cells=Pieces.cells(kind)
            val w=cells.maxOf { it.x }-cells.minOf { it.x }+1
            val h=cells.maxOf { it.y }-cells.minOf { it.y }+1
            val cell=min(size.width/4.5f,size.height/2.6f)
            val ox=(size.width-w*cell)/2-cells.minOf{it.x}*cell
            val oy=(size.height-h*cell)/2-cells.minOf{it.y}*cell
            cells.forEach { block(Offset(ox+it.x*cell,oy+it.y*cell),cell,kind,alpha) }
        }
    }
}
