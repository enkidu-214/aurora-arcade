package com.aurora.arcade.core.games

/** World coordinates are shared by collision simulation and the projected table renderer. */
data class PinballPoint(val x: Double, val y: Double, val z: Double = 0.0)
data class PinballSegment(val ax: Double, val ay: Double, val bx: Double, val by: Double)
data class PinballCircle(val id: Int, val x: Double, val y: Double, val radius: Double)
data class PinballBall(
    val id: Int, val x: Double, val y: Double, val vx: Double, val vy: Double,
    val z: Double = 0.0, val track: Int = -1, val trackProgress: Double = 0.0,
)
enum class PinballMission { BUMPERS, TARGETS, RAMPS, CORE }
enum class PinballEventKind { FLIPPER, LAUNCH, BUMPER, SLING, TARGET, RAMP, SKILL, COMBO, MISSION, MULTIBALL, JACKPOT, SAVE, DRAIN, NUDGE, GAME_OVER }
data class PinballEvent(
    val id: Long, val kind: PinballEventKind, val x: Double, val y: Double,
    val timeMs: Long, val points: Int = 0, val label: String = "",
)
data class PinballState(
    val balls: List<PinballBall> = emptyList(),
    val score: Int = 0, val lives: Int = 3, val sector: Int = 1,
    val awaitingLaunch: Boolean = true, val gameOver: Boolean = false,
    val leftFlipper: Double = 0.0, val rightFlipper: Double = 0.0,
    val charge: Double = 0.0, val energy: Int = 0,
    val combo: Int = 0, val comboRemainingMs: Long = 0,
    val multiplier: Int = 1, val targetMask: Int = 0, val laneMask: Int = 0,
    val mission: PinballMission = PinballMission.BUMPERS,
    val missionProgress: Int = 0, val missionGoal: Int = 6,
    val novaRemainingMs: Long = 0, val ballSaveMs: Long = 0,
    val nudgeCooldownMs: Long = 0, val elapsedMs: Long = 0,
)
