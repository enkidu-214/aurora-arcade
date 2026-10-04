package com.aurora.arcade.ui.games

import android.graphics.Paint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aurora.arcade.core.games.*
import kotlin.math.*

private val Ice = Color(0xFF70E7EF)
private val Gold = Color(0xFFFFCE84)
private val Violet = Color(0xFFB69AF7)

internal class PinballVisuals {
    val clock = mutableLongStateOf(0L)
    val contacts = ArrayList<PinballFlash>()
    val trails = ArrayList<PinballTrace>()
    fun update(now: Long, state: PinballState, events: List<PinballEvent>, reduced: Boolean) {
        clock.longValue = now
        contacts.removeAll { now - it.born > 1_050_000_000L }
        events.filter { it.kind != PinballEventKind.FLIPPER }.forEach { contacts.add(PinballFlash(it, now)) }
        while (contacts.size > 26) contacts.removeAt(0)
        trails.removeAll { now - it.born > 150_000_000L }
        if (reduced) trails.clear() else state.balls.forEach { trails.add(PinballTrace(it.id, PinballPoint(it.x, it.y, it.z), now)) }
        while (trails.size > 42) trails.removeAt(0)
    }
}
internal data class PinballFlash(val event: PinballEvent, val born: Long)
internal data class PinballTrace(val ball: Int, val point: PinballPoint, val born: Long)

/** A fixed, gently tapered camera. Every rail, ramp and ball uses the physics world. */
private class TableProjection(val width: Float, val height: Float) {
    val unit = min(width / 1040f, height / 1740f)
    val origin = Offset(width / 2, (height - 1740 * unit) / 2 + 45 * unit)
    fun at(x: Double, y: Double, z: Double = 0.0): Offset {
        val depth = .79 + .21 * (y / PinballTable.HEIGHT).coerceIn(0.0, 1.0)
        return origin + Offset(((x - 500) * depth * unit).toFloat(), ((y * .91 - z * .55) * unit).toFloat())
    }
    fun at(p: PinballPoint) = at(p.x, p.y, p.z)
    fun radius(r: Double, y: Double) = (r * unit * (.79 + .21 * y / PinballTable.HEIGHT)).toFloat()
}

@Composable
internal fun PinballCanvas(engine: PinballEngine, visuals: PinballVisuals, reducedMotion: Boolean, modifier: Modifier) {
    Spacer(modifier.drawWithCache {
        val p = TableProjection(size.width, size.height)
        fun polygon(vararg points: PinballPoint) = Path().apply { points.forEachIndexed { i, at -> val v = p.at(at); if (i == 0) moveTo(v.x, v.y) else lineTo(v.x, v.y) }; close() }
        val table = polygon(PinballPoint(20.0, 0.0), PinballPoint(980.0, 0.0), PinballPoint(980.0, 1780.0), PinballPoint(20.0, 1780.0))
        val cabinet = polygon(PinballPoint(-5.0, 0.0), PinballPoint(1005.0, 0.0), PinballPoint(1005.0, 1810.0), PinballPoint(-5.0, 1810.0))
        val rampPaths = (0..2).map { track -> Path().apply {
            (0..64).forEach { step -> val at = p.at(PinballTable.trackPoint(track, step / 64.0)); if (step == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y) }
        } }
        val trackShadows = (0..2).map { track -> Path().apply {
            (0..64).forEach { step -> val world = PinballTable.trackPoint(track, step / 64.0); val at = p.at(world.x, world.y); if (step == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y) }
        } }
        val grid = Path().apply {
            for (x in 100..900 step 100) { val a = p.at(x.toDouble(), 100.0); val b = p.at(x.toDouble(), 1710.0); moveTo(a.x, a.y); lineTo(b.x, b.y) }
            for (y in 100..1700 step 100) { val a = p.at(80.0, y.toDouble()); val b = p.at(900.0, y.toDouble()); moveTo(a.x, a.y); lineTo(b.x, b.y) }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL); textAlign = Paint.Align.CENTER }
        val background = Brush.verticalGradient(listOf(Color(0xFF172330), Color(0xFF0C1724), Color(0xFF1B2A36)))
        onDrawBehind {
            // Reading the clock here invalidates drawing only, never the surrounding UI tree.
            val now = visuals.clock.longValue
            val s = engine.state
            val u = p.unit
            fun label(text: String, x: Double, y: Double, color: Color, worldSize: Float = 29f) {
                val at = p.at(x, y)
                paint.color = color.toArgb(); paint.textSize = worldSize * u
                drawContext.canvas.nativeCanvas.drawText(text, at.x, at.y, paint)
            }
            drawRect(Color(0xFF0B1420))
            drawPath(cabinet, Color.Black.copy(alpha = .75f))
            drawPath(cabinet, Color(0xFF354C5B), style = Stroke(13 * u))
            drawPath(table, background)
            drawPath(grid, Color(0xFF547080).copy(alpha = .13f), style = Stroke(u))
            // Etched field artwork stays below all interactive objects.
            drawCircle(Brush.radialGradient(listOf(Ice.copy(alpha = .055f), Color.Transparent), p.at(500.0, 750.0), 550 * u), 550 * u, p.at(500.0, 750.0))
            for (ring in listOf(215f, 330f, 445f)) drawCircle(Color(0xFF243A49), ring * u, p.at(500.0, 920.0), style = Stroke(u))
            label("SUPER NOVA", 490.0, 55.0, Gold, 38f)
            label("REACTOR  /  SECTOR ${s.sector.toString().padStart(2, '0')}", 490.0, 88.0, Color(0xFF8297A9), 20f)
            // Lane gate lamps and their progression are positioned along the upper orbit.
            for (i in 0..2) {
                val at = p.at(280.0 + i * 220.0, 250.0)
                val lit = s.laneMask and (1 shl i) != 0
                drawCircle(if (lit) Gold.copy(alpha = .14f) else Color(0xFF223847), 19 * u, at)
                drawCircle(if (lit) Gold else Color(0xFF486273), 8 * u, at)
            }
            // Exact rails: dark extruded underside, metallic face, thin lit upper lip.
            PinballTable.walls.forEach { rail ->
                val a = p.at(rail.ax, rail.ay); val b = p.at(rail.bx, rail.by)
                drawLine(Color.Black.copy(alpha = .65f), a + Offset(4 * u, 9 * u), b + Offset(4 * u, 9 * u), 16 * u, StrokeCap.Round)
                drawLine(Color(0xFF435E6D), a, b, 13 * u, StrokeCap.Round)
                drawLine(Color(0xFFADC4CD), p.at(rail.ax, rail.ay, 12.0), p.at(rail.bx, rail.by, 12.0), 3 * u, StrokeCap.Round)
            }
            // Raised tracks use the same elevated path as captured balls, including shadows.
            rampPaths.forEachIndexed { track, path ->
                val color = if (track == 1) Ice else if (track == 2) Violet else Gold
                drawPath(trackShadows[track], Color.Black.copy(alpha = .6f), style = Stroke(if (track == 0) 34 * u else 48 * u, cap = StrokeCap.Round))
                drawPath(path, Color(0xFF456071), style = Stroke(if (track == 0) 27 * u else 38 * u, cap = StrokeCap.Round))
                drawPath(path, Color(0xFF172837), style = Stroke(if (track == 0) 21 * u else 30 * u, cap = StrokeCap.Round))
                drawPath(path, color.copy(alpha = .55f), style = Stroke(2 * u, cap = StrokeCap.Round))
            }
            // Ramp mouth arrows pulse gently only when this is the current mission.
            for ((x, color) in listOf(185.0 to Ice, 815.0 to Violet)) {
                val lit = s.mission == PinballMission.RAMPS || s.novaRemainingMs > 0
                val arrowAlpha = if (!lit) .3f else if (reducedMotion) .95f else (.7 + .25 * sin(now / 260_000_000.0)).toFloat()
                for (i in 0..2) {
                    val y = 980.0 + i * 50
                    drawLine(color.copy(alpha = arrowAlpha), p.at(x - 20, y + 18), p.at(x, y), 6 * u, StrokeCap.Round)
                    drawLine(color.copy(alpha = arrowAlpha), p.at(x, y), p.at(x + 20, y + 18), 6 * u, StrokeCap.Round)
                }
                label(if (x < 500) "RAMP" else "ORBIT", x, 1170.0, color.copy(alpha = .75f), 22f)
            }
            PinballTable.slings.forEach { sling ->
                drawLine(Color.Black.copy(alpha = .7f), p.at(sling.ax, sling.ay) + Offset(0f, 8 * u), p.at(sling.bx, sling.by) + Offset(0f, 8 * u), 23 * u, StrokeCap.Round)
                drawLine(Color(0xFF546476), p.at(sling.ax, sling.ay), p.at(sling.bx, sling.by), 16 * u, StrokeCap.Round)
                drawLine(Violet, p.at(sling.ax, sling.ay, 12.0), p.at(sling.bx, sling.by, 12.0), 5 * u, StrokeCap.Round)
            }
            PinballTable.bumpers.forEach { bumper ->
                val base = p.at(bumper.x, bumper.y)
                val top = p.at(bumper.x, bumper.y, 30.0)
                val r = p.radius(bumper.radius, bumper.y)
                val hit = visuals.contacts.lastOrNull { it.event.kind == PinballEventKind.BUMPER && abs(it.event.x - bumper.x) < 80 && abs(it.event.y - bumper.y) < 80 }
                val flash = hit?.let { (1 - (now - it.born) / 250_000_000f).coerceIn(0f, 1f) } ?: 0f
                val lit = s.mission == PinballMission.BUMPERS
                drawCircle(Color.Black.copy(alpha = .65f), r + 6 * u, base + Offset(4 * u, 10 * u))
                drawCircle(Color(0xFF355362), r, base)
                drawCircle(Ice.copy(alpha = if (lit) .14f + flash * .4f else .07f), r + 17 * u, top)
                drawCircle(Color(0xFF9CBAC3), r, top)
                drawCircle(Color(0xFF233A4A), r - 4 * u, top)
                drawCircle(if (flash > 0) Color(0xFFE1FFFF) else Ice, r - 13 * u, top, style = Stroke(5 * u))
                drawCircle(Brush.radialGradient(listOf(Ice.copy(alpha = .65f), Color(0xFF203B4C)), top - Offset(12 * u, 14 * u), r), r - 19 * u, top)
                drawCircle(Color(0xFFE4FFFF), 5 * u, top - Offset(10 * u, 12 * u))
            }
            PinballTable.targets.forEach { target ->
                val at = p.at(target.x, target.y, 8.0)
                val lit = s.targetMask and (1 shl target.id) != 0
                val r = p.radius(target.radius + 6, target.y)
                drawCircle(Color.Black.copy(alpha = .7f), r + 3 * u, at + Offset(3 * u, 6 * u))
                drawCircle(if (lit) Gold else Color(0xFF637584), r, at)
                drawCircle(if (lit) Color(0xFFFFE5AD) else Color(0xFF263F51), r - 5 * u, at)
                label((target.id + 1).toString(), target.x, target.y + 10, if (lit) Color(0xFF3C3429) else Gold, 27f)
                if (s.mission == PinballMission.TARGETS && !lit) drawCircle(Gold.copy(alpha = .5f), r + 10 * u, at, style = Stroke(2 * u))
            }
            val core = PinballTable.core
            val center = p.at(core.x, core.y, 18.0)
            val nova = s.novaRemainingMs > 0
            val coreColor = if (nova) Gold else Violet
            drawCircle(Color.Black.copy(alpha = .6f), 72 * u, center + Offset(0f, 12 * u))
            drawCircle(Color(0xFF314251), 67 * u, center)
            drawCircle(coreColor.copy(alpha = .3f), 58 * u, center, style = Stroke(6 * u))
            drawCircle(Brush.radialGradient(listOf(coreColor.copy(alpha = .8f), Color(0xFF172735)), center - Offset(12 * u, 12 * u), 42 * u), 41 * u, center)
            drawCircle(Color.White.copy(alpha = .65f), 10 * u, center - Offset(11 * u, 12 * u))
            if (s.mission == PinballMission.CORE || nova) drawCircle(coreColor, 80 * u, center, style = Stroke(2 * u))
            label(if (nova) "JACKPOT" else "REACTOR", core.x, core.y + 115, coreColor, 26f)
            // Flipper faces and pivots follow the same moving capsules as collisions.
            for (left in listOf(true, false)) {
                val flipper = PinballTable.flipper(left, if (left) s.leftFlipper else s.rightFlipper)
                val a = p.at(flipper.ax, flipper.ay); val b = p.at(flipper.bx, flipper.by)
                drawLine(Color.Black.copy(alpha = .8f), a + Offset(3 * u, 9 * u), b + Offset(3 * u, 9 * u), 28 * u, StrokeCap.Round)
                drawLine(Color(0xFF53707E), a, b, 24 * u, StrokeCap.Round)
                drawLine(Color(0xFFC1D6DE), a - Offset(0f, 3 * u), b - Offset(0f, 3 * u), 20 * u, StrokeCap.Round)
                drawLine(Ice, a - Offset(0f, 6 * u), b - Offset(0f, 6 * u), 8 * u, StrokeCap.Round)
                drawCircle(Color(0xFF223A48), 12 * u, a)
                drawCircle(Color(0xFFA7C5CE), 6 * u, a)
            }
            val energyLeft = p.at(280.0, 1700.0); val energyRight = p.at(720.0, 1700.0)
            drawLine(Color(0xFF2F4553), energyLeft, energyRight, 10 * u, StrokeCap.Round)
            drawLine(if (s.energy >= 100 || nova) Gold else Ice, energyLeft, energyLeft + (energyRight - energyLeft) * (s.energy / 100f), 6 * u, StrokeCap.Round)
            label(if (nova) "NOVA ACTIVE  /  ×2" else "NOVA ENERGY  ${s.energy}%", 500.0, 1760.0, if (nova) Gold else Color(0xFF88A4B4), 26f)
            if (s.ballSaveMs > 0) label("BALL SAVE  ${(s.ballSaveMs + 999) / 1000}s", 500.0, 1435.0, Ice, 28f)
            if (!reducedMotion) visuals.trails.forEach { trace ->
                val alpha = (1 - (now - trace.born) / 150_000_000f).coerceIn(0f, 1f)
                drawCircle((if (nova) Gold else Ice).copy(alpha = alpha * .3f), (6 + alpha * 9) * u, p.at(trace.point))
            }
            s.balls.sortedBy { it.z }.forEach { ball ->
                val at = p.at(ball.x, ball.y, ball.z + 6)
                val r = p.radius(PinballTable.BALL_RADIUS, ball.y)
                val shadow = p.at(ball.x + 9, ball.y + 15)
                drawOval(Color.Black.copy(alpha = .7f), shadow - Offset(r, r * .65f), Size(r * 2.4f, r * 1.5f))
                if (nova) drawCircle(Gold.copy(alpha = .2f), r * 1.8f, at)
                drawCircle(Color(0xFF213845), r + u, at)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD5E6ED), Color(0xFF8CABC0), Color(0xFF233B51)), at - Offset(r * .35f, r * .4f), r * 1.6f), r, at)
                drawCircle(Color.White, r * .2f, at - Offset(r * .3f, r * .4f))
            }
            visuals.contacts.forEach { flash ->
                val event = flash.event
                val progress = ((now - flash.born) / 1_050_000_000f).coerceIn(0f, 1f)
                val color = when (event.kind) { PinballEventKind.JACKPOT, PinballEventKind.MULTIBALL, PinballEventKind.SKILL -> Gold; PinballEventKind.MISSION -> Violet; else -> Ice }
                val at = p.at(event.x, event.y)
                if (!reducedMotion && progress < .5f) {
                    drawCircle(color.copy(alpha = (1 - progress * 2) * .5f), (30 + progress * 190) * u, at, style = Stroke(4 * u))
                    for (i in 0..5) { val angle = i * PI / 3; val offset = Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (35 + progress * 170) * u; drawLine(color.copy(alpha = (1 - progress * 2) * .8f), at + offset, at + offset * 1.12f, 3 * u) }
                }
                if (event.points > 0 && progress < .85f) {
                    val scoreAt = at - Offset(0f, if (reducedMotion) 25 * u else (20 + progress * 90) * u)
                    paint.color = color.copy(alpha = ((.85f - progress) * 2).coerceIn(0f, 1f)).toArgb(); paint.textSize = 29 * u
                    drawContext.canvas.nativeCanvas.drawText("+${event.points}", scoreAt.x, scoreAt.y, paint)
                }
            }
            val celebration = visuals.contacts.lastOrNull { it.event.kind in listOf(PinballEventKind.MULTIBALL, PinballEventKind.JACKPOT, PinballEventKind.MISSION, PinballEventKind.SKILL, PinballEventKind.SAVE) }
            if (celebration != null && now - celebration.born < 1_050_000_000L) {
                label(celebration.event.label.ifBlank { when (celebration.event.kind) { PinballEventKind.JACKPOT -> "JACKPOT"; PinballEventKind.MULTIBALL -> "SUPERNOVA"; PinballEventKind.SAVE -> "BALL SAVED"; else -> "MISSION COMPLETE" } }, 500.0, 1260.0, Gold, 34f)
            }
            if (s.awaitingLaunch && !s.gameOver) {
                val waiting = p.at(950.0, 1640.0)
                val r = p.radius(PinballTable.BALL_RADIUS, 1640.0)
                drawCircle(Color.Black.copy(alpha = .7f), r * 1.2f, waiting + Offset(2 * u, 5 * u))
                drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFB2CAD8), Color(0xFF324B61)), waiting - Offset(r * .35f, r * .4f), r * 1.7f), r, waiting)
                drawLine(Gold.copy(alpha = .4f), p.at(950.0, 1740.0), p.at(950.0, 1680.0), 12 * u, StrokeCap.Round)
            }
            if (s.awaitingLaunch || s.gameOver) {
                label(if (s.gameOver) "本轮结束" else "准备发射", 500.0, 1290.0, Gold, 42f)
                label(if (s.gameOver) "再来一局 · 挑战更高记录" else "按住蓄力 · 松手开球", 500.0, 1345.0, Color(0xFFB9CBD6), 27f)
                if (s.awaitingLaunch) {
                    val a = p.at(350.0, 1390.0); val b = p.at(650.0, 1390.0)
                    drawLine(Color(0xFF3C4E5C), a, b, 13 * u, StrokeCap.Round)
                    drawLine(Ice.copy(alpha = .7f), a + (b - a) * .65f, a + (b - a) * .88f, 13 * u)
                    val charge = a + (b - a) * s.charge.toFloat()
                    drawLine(Gold, a, charge, 5 * u, StrokeCap.Round)
                    drawCircle(Color.White, 9 * u, charge)
                    label("青色区松手 · 技巧奖励", 500.0, 1417.0, Ice, 23f)
                }
            }
        }
    })
}
