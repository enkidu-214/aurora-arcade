package com.aurora.arcade.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.arcade.GameSession
import com.aurora.arcade.core.Action

@Composable fun ControlKey(session: GameSession,action: Action,icon: String,label: String,
    modifier: Modifier=Modifier,enabled: Boolean=true,showLabel: Boolean=false,
    iconSize: Dp=if(showLabel) 22.dp else 26.dp,stackedLabel: String?=null) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if(pressed) .95f else 1f,spring(dampingRatio=.65f,stiffness=1000f),label="press")
    Box(modifier.graphicsLayer { scaleX=scale; scaleY=scale; alpha=if(enabled) 1f else .40f }
        .semantics { contentDescription=label; role=Role.Button; if(!enabled) disabled()
            onClick { if(enabled) { session.tap(action); true } else false } }
        .pointerInput(action,enabled) {
            if(!enabled) return@pointerInput
            awaitEachGesture {
                val down=awaitFirstDown(requireUnconsumed=false)
                down.consume(); pressed=true; session.press(action)
                try {
                    while(true) {
                        val event=awaitPointerEvent()
                        val point=event.changes.firstOrNull { it.id == down.id } ?: break
                        if(!point.pressed || point.position.x<0 || point.position.x>size.width || point.position.y<0 || point.position.y>size.height) break
                        point.consume()
                    }
                } finally { pressed=false; session.release(action) }
            }
        },contentAlignment=Alignment.Center) {
        CandyKeySurface(icon,pressed,Modifier.matchParentSize())
        if(stackedLabel!=null) {
            Column(Modifier.padding(bottom=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(1.dp)) {
                CuteControlIcon(icon,Modifier.size(iconSize))
                Text(stackedLabel,color=Ink,fontSize=10.sp,fontWeight=FontWeight.Medium,maxLines=1)
            }
        } else {
            Row(Modifier.padding(bottom=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                CuteControlIcon(icon,Modifier.size(iconSize))
                if(showLabel) { Spacer(Modifier.width(6.dp)); Text(label,color=Ink,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=1) }
            }
        }
    }
}

@Composable fun GameIcon(name: String,color: Color= Cream,modifier: Modifier=Modifier) {
    Canvas(modifier) {
        val unit=size.minDimension/24f
        fun point(x: Float,y: Float)=Offset(x*unit,y*unit)
        fun line(x: Float,y: Float,x2: Float,y2: Float)=drawLine(color,point(x,y),point(x2,y2),1.8f*unit,StrokeCap.Round)
        when(name) {
            "left","back" -> { line(18f,12f,6f,12f); line(6f,12f,11f,7f); line(6f,12f,11f,17f) }
            "right" -> { line(6f,12f,18f,12f); line(18f,12f,13f,7f); line(18f,12f,13f,17f) }
            "down","drop" -> { line(12f,5f,12f,17f); line(7f,12f,12f,17f); line(17f,12f,12f,17f); if(name=="drop") line(5f,21f,19f,21f) }
            "pause" -> { line(8f,6f,8f,18f); line(16f,6f,16f,18f) }
            "play" -> { val p=Path().apply { moveTo(8*unit,5*unit); lineTo(19*unit,12*unit); lineTo(8*unit,19*unit); close() }; drawPath(p,color,style=Stroke(unit*1.7f)) }
            "close" -> { line(6f,6f,18f,18f); line(18f,6f,6f,18f) }
            "settings" -> { line(5f,7f,19f,7f);line(5f,17f,19f,17f);drawCircle(Ink,3*unit,point(9f,7f));drawCircle(color,3*unit,point(9f,7f),style=Stroke(1.5f*unit));drawCircle(Ink,3*unit,point(15f,17f));drawCircle(color,3*unit,point(15f,17f),style=Stroke(1.5f*unit)) }
            "undo","ccw","cw" -> {
                val clockwise=name=="cw"
                drawArc(color,-90f,if(clockwise) 270f else -270f,false,point(5f,5f),androidx.compose.ui.geometry.Size(14*unit,14*unit),style=Stroke(1.8f*unit,cap=StrokeCap.Round))
                if(clockwise) { line(2f,15f,5f,12f);line(5f,12f,8f,15f) } else { line(16f,15f,19f,12f);line(19f,12f,22f,15f) }
            }
            else -> { drawCircle(color,2*unit,point(12f,12f));drawCircle(color,8*unit,point(12f,12f),style=Stroke(unit)) }
        }
    }
}
