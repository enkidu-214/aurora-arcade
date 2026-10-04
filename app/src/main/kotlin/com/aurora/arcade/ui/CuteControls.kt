package com.aurora.arcade.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

private val SpriteInk = Color(0xFF26364D)
private val SpriteCream = Color(0xFFDFE6E9)

private fun candyColor(name: String) = when(name) {
    "undo", "up" -> Color(0xFFA8A6B9)
    "drop" -> Color(0xFF8EB1AA)
    else -> Color(0xFF96ADBB)
}

// Rounded capsules keep the entire original rectangle available for touch input.
@Composable internal fun CandyKeySurface(name: String,pressed: Boolean,modifier: Modifier) {
    Canvas(modifier) {
        val color = candyColor(name)
        val edge = 1.dp.toPx()
        val depth = 5.dp.toPx()
        val radius = CornerRadius(size.height/2)
        val faceTop = if(pressed) depth*.65f else 0f
        drawRoundRect(lerp(color,Ink,.58f),Offset(edge,depth),Size(size.width-edge*2,size.height-depth),radius)
        val at = Offset(edge,faceTop)
        val face = Size(size.width-edge*2,size.height-depth)
        drawRoundRect(Brush.verticalGradient(listOf(lerp(color,SpriteCream,.10f),color),faceTop,size.height),at,face,radius)
        drawRoundRect(SpriteCream.copy(alpha=.24f),at,face,radius,style=Stroke(edge))
        drawLine(SpriteCream.copy(alpha=.30f),Offset(size.height*.36f,faceTop+4.dp.toPx()),Offset(size.width-size.height*.36f,faceTop+4.dp.toPx()),1.4.dp.toPx(),StrokeCap.Round)
    }
}

private val downSprite = Path().apply {
    moveTo(11f,3f); lineTo(21f,3f); lineTo(21f,13f); lineTo(29f,13f)
    lineTo(29f,17f); lineTo(25f,17f); lineTo(25f,21f); lineTo(21f,21f)
    lineTo(21f,25f); lineTo(18f,25f); lineTo(18f,29f); lineTo(14f,29f)
    lineTo(14f,25f); lineTo(11f,25f); lineTo(11f,21f); lineTo(7f,21f)
    lineTo(7f,17f); lineTo(3f,17f); lineTo(3f,13f); lineTo(11f,13f); close()
}

private val clockwiseSprite = Path().apply {
    moveTo(11f,4f); lineTo(22f,4f); lineTo(22f,6f); lineTo(25f,6f)
    lineTo(25f,9f); lineTo(26f,9f); lineTo(26f,11f); lineTo(30f,11f)
    lineTo(30f,14f); lineTo(28f,14f); lineTo(28f,16f); lineTo(26f,16f)
    lineTo(26f,18f); lineTo(24f,18f); lineTo(24f,16f); lineTo(22f,16f)
    lineTo(22f,14f); lineTo(20f,14f); lineTo(20f,11f); lineTo(22f,11f)
    lineTo(22f,10f); lineTo(20f,10f); lineTo(20f,8f); lineTo(12f,8f)
    lineTo(12f,10f); lineTo(10f,10f); lineTo(10f,12f); lineTo(8f,12f)
    lineTo(8f,21f); lineTo(10f,21f); lineTo(10f,23f); lineTo(12f,23f)
    lineTo(12f,25f); lineTo(20f,25f); lineTo(20f,23f); lineTo(22f,23f)
    lineTo(22f,21f); lineTo(26f,21f); lineTo(26f,24f); lineTo(24f,24f)
    lineTo(24f,26f); lineTo(22f,26f); lineTo(22f,29f); lineTo(10f,29f)
    lineTo(10f,27f); lineTo(7f,27f); lineTo(7f,24f); lineTo(4f,24f)
    lineTo(4f,10f); lineTo(7f,10f); lineTo(7f,7f); lineTo(11f,7f); close()
}

@Composable internal fun CuteControlIcon(name: String,modifier: Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension/32f
        withTransform({
            translate((size.width-32f*unit)/2,(size.height-32f*unit)/2)
            scale(unit,unit,pivot=Offset.Zero)
        }) {
            when(name) {
                "ccw", "cw" -> {
                    withTransform({ if(name=="ccw") scale(-1f,1f,pivot=Offset(16f,16f)) }) { sticker(clockwiseSprite) }
                }
                "undo", "up" -> {
                    rail(4f)
                    withTransform({ translate(0f,2f); scale(.80f,.80f,pivot=Offset(16f,16f)) }) { arrow("up") }
                }
                "drop" -> {
                    withTransform({ translate(0f,-2f); scale(.80f,.80f,pivot=Offset(16f,16f)) }) { arrow("down") }
                    rail(28f)
                }
                else -> arrow(name)
            }
        }
    }
}

private fun DrawScope.sticker(path: Path) {
    withTransform({ translate(0f,1.1f) }) {
        drawPath(path,SpriteInk.copy(alpha=.24f),style=Stroke(2.4f,join=StrokeJoin.Round))
    }
    drawPath(path,SpriteCream)
    drawPath(path,SpriteInk,style=Stroke(1.5f,join=StrokeJoin.Round))
}

private fun DrawScope.arrow(direction: String) {
    val rotation = when(direction) { "left" -> 90f; "right" -> -90f; "up" -> 180f; else -> 0f }
    withTransform({ rotate(rotation,Offset(16f,16f)) }) { sticker(downSprite) }
}

private fun DrawScope.rail(y: Float) {
    drawLine(SpriteInk,Offset(7f,y),Offset(25f,y),3.8f,StrokeCap.Round)
    drawLine(SpriteCream,Offset(8f,y-.2f),Offset(24f,y-.2f),1.6f,StrokeCap.Round)
}
