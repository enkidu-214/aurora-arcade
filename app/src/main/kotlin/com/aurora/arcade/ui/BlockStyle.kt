package com.aurora.arcade.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aurora.arcade.core.Kind

// Samples from the official web game's normal minos: face, rim and lower highlight.
// Keep the face free of decorative patterns at every size, including previews.
internal fun DrawScope.block(pos: Offset,cell: Float,kind: Kind,alpha: Float=1f,glow: Boolean=false) {
    val palette = PiecePalette[kind.ordinal]
    val inset = cell*.035f
    val at = pos+Offset(inset,inset)
    val box = Size(cell-inset*2,cell-inset*2)
    val radius = CornerRadius(cell*.035f)
    if(glow) drawRoundRect(palette.face.copy(alpha=.12f*alpha),pos-Offset(1f,1f),Size(cell+2f,cell+2f),radius)
    drawRoundRect(palette.rim.copy(alpha=alpha),at,box,radius)
    val edge = cell*.085f
    val faceAt = at+Offset(edge,edge)
    val faceSize = Size(box.width-edge*2,box.height-edge*2)
    drawRoundRect(Brush.verticalGradient(
        0f to palette.upper.copy(alpha=alpha),
        .5f to palette.face.copy(alpha=alpha),
        1f to palette.lower.copy(alpha=alpha),
        startY=faceAt.y,endY=faceAt.y+faceSize.height,
    ),faceAt,faceSize,CornerRadius(cell*.02f))
    drawRoundRect(palette.outline.copy(alpha=alpha),at,box,radius,style=Stroke(maxOf(.65f,cell*.025f)))
    drawLine(Color.White.copy(alpha=.28f*alpha),faceAt,faceAt+Offset(faceSize.width,0f),maxOf(.65f,cell*.028f))
}
