package com.aurora.arcade.core.games

import kotlin.math.*

/** One source of truth for both the projected renderer and collision world. */
object PinballTable {
    const val WIDTH = 1000.0
    const val HEIGHT = 1800.0
    const val BALL_RADIUS = 18.0
    val walls = listOf(
        PinballSegment(100.0,180.0,220.0,100.0), PinballSegment(220.0,100.0,780.0,100.0),
        PinballSegment(780.0,100.0,900.0,180.0), PinballSegment(100.0,180.0,100.0,1150.0),
        PinballSegment(900.0,180.0,900.0,1150.0), PinballSegment(100.0,1150.0,210.0,1470.0),
        PinballSegment(210.0,1470.0,270.0,1530.0), PinballSegment(900.0,1150.0,790.0,1470.0),
        PinballSegment(790.0,1470.0,730.0,1530.0),
        PinballSegment(62.0,1100.0,62.0,1818.0), PinballSegment(938.0,1100.0,938.0,1818.0),
    )
    val bumpers = listOf(PinballCircle(0,355.0,550.0,55.0),PinballCircle(1,645.0,550.0,55.0),PinballCircle(2,500.0,790.0,58.0))
    val targets = listOf(PinballCircle(0,185.0,650.0,25.0),PinballCircle(1,815.0,650.0,25.0),PinballCircle(2,500.0,330.0,25.0))
    val core = PinballCircle(0,500.0,1050.0,38.0)
    val slings = listOf(PinballSegment(180.0,1250.0,310.0,1420.0),PinballSegment(820.0,1250.0,690.0,1420.0))
    fun flipper(left: Boolean, amount: Double): PinballSegment {
        val angle=Math.toRadians(22.0-49.0*amount.coerceIn(0.0,1.0))
        val x=if(left)270.0 else 730.0; val direction=if(left)1 else -1
        return PinballSegment(x,1530.0,x+direction*195*cos(angle),1530.0+195*sin(angle))
    }
    private val paths = listOf(
        listOf(PinballPoint(950.0,1640.0),PinballPoint(950.0,900.0),PinballPoint(950.0,300.0),PinballPoint(875.0,180.0),PinballPoint(720.0,190.0),PinballPoint(640.0,310.0)),
        listOf(PinballPoint(185.0,960.0),PinballPoint(145.0,500.0),PinballPoint(220.0,210.0),PinballPoint(420.0,160.0),PinballPoint(680.0,350.0),PinballPoint(790.0,730.0),PinballPoint(740.0,1180.0)),
        listOf(PinballPoint(815.0,960.0),PinballPoint(855.0,500.0),PinballPoint(780.0,210.0),PinballPoint(580.0,160.0),PinballPoint(320.0,350.0),PinballPoint(210.0,730.0),PinballPoint(260.0,1180.0)),
    )
    private val lengths = paths.map { p -> p.zipWithNext { a,b -> hypot(a.x-b.x,a.y-b.y) } }
    /** Arc-length interpolation avoids speed jumps at rail joins. z is shared with rendering. */
    fun trackPoint(track: Int, progress: Double): PinballPoint {
        val id=track.coerceIn(0,2);val p=progress.coerceIn(0.0,1.0);val path=paths[id]
        var distance=lengths[id].sum()*p
        for(i in lengths[id].indices) {
            val length=lengths[id][i]
            if(distance<=length || i==lengths[id].lastIndex) {
                val t=(distance/length).coerceIn(0.0,1.0);val a=path[i];val b=path[i+1]
                return PinballPoint(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,if(id==0)0.0 else 90*sin(PI*p))
            };distance-=length
        }
        return path.last()
    }
}
