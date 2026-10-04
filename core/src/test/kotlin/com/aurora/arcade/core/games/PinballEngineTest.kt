package com.aurora.arcade.core.games

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PinballEngineTest {
    private fun fixture(s: PinballState): PinballEngine = PinballEngine(42).also {
        it.javaClass.getDeclaredField("state").apply { isAccessible = true }.set(it, s)
    }
    private fun ball(x: Double, y: Double, vx: Double = 0.0, vy: Double = 0.0, id: Int = 1) = PinballBall(id,x,y,vx,vy)
    private fun run(e: PinballEngine, ms: Int) { repeat(ms / 10) { e.advance(10) } }
    @Test fun tapLaunchTravelsUpperPlayfieldAndBallSaveUsesActiveTime() {
        val e=PinballEngine(1); assertTrue(e.releaseCharge()); assertFalse(e.releaseCharge())
        assertEquals(8000,e.state.ballSaveMs); run(e,1500)
        assertTrue(e.state.balls.any { it.y < 600 }); assertTrue(e.state.ballSaveMs in 6400..6500)
        assertFalse(e.state.awaitingLaunch)
    }
    @Test fun lastBallDrainSpendsExactlyOneLifeAndSavedBallRelaunches() {
        val e=fixture(PinballState(balls=listOf(ball(500.0,1810.0,vy=600.0)),awaitingLaunch=false))
        e.advance(20); assertEquals(2,e.state.lives); assertTrue(e.state.awaitingLaunch)
        val saved=fixture(PinballState(balls=listOf(ball(500.0,1810.0,vy=600.0)),awaitingLaunch=false,ballSaveMs=500))
        saved.advance(20); assertEquals(3,saved.state.lives); assertEquals(0,saved.state.balls.single().track)
        val multi=fixture(PinballState(balls=listOf(ball(500.0,1810.0,vy=600.0),ball(600.0,700.0,id=2)),awaitingLaunch=false))
        multi.advance(20); assertEquals(3,multi.state.lives); assertEquals(1,multi.state.balls.size)
    }
    @Test fun movingFlipperLaunchesButHeldFlipperDoesNotInjectRepeatedImpulse() {
        val segment=PinballTable.flipper(true,0.0)
        val x=segment.ax+(segment.bx-segment.ax)*.65; val y=segment.ay+(segment.by-segment.ay)*.65-30
        val e=fixture(PinballState(balls=listOf(ball(x,y,vy=160.0)),awaitingLaunch=false))
        e.setFlippers(true,false); run(e,90); assertTrue("Moving flipper must shoot upward: ${e.state.balls}",e.state.balls.single().vy < -500)
        val held=fixture(PinballState(balls=listOf(ball(390.0,1380.0,vy=100.0)),awaitingLaunch=false,leftFlipper=1.0))
        held.setFlippers(true,false); run(held,200); assertTrue(hypot(held.state.balls.single().vx,held.state.balls.single().vy)<700)
    }
    @Test fun bumperContactScoresOnceAndSeparationPreventsOverlapFarming() {
        val c=PinballTable.bumpers.first(); val e=fixture(PinballState(balls=listOf(ball(c.x,c.y+c.radius+17,vy=-500.0)),awaitingLaunch=false))
        e.advance(10); assertEquals(1,e.state.missionProgress); assertTrue(e.state.energy>0)
        val score=e.state.score; e.advance(10); assertEquals(score,e.state.score)
        assertTrue(hypot(e.state.balls.single().x-c.x,e.state.balls.single().y-c.y)>=c.radius+18-0.1)
    }
    @Test fun nudgeAndNovaAreEdgeActionsWithCapsAndTimers() {
        val e=fixture(PinballState(balls=listOf(ball(500.0,1250.0)),awaitingLaunch=false,energy=100))
        assertTrue(e.nudge()); assertFalse(e.nudge()); assertTrue(e.activateNova()); assertFalse(e.activateNova())
        assertEquals(3,e.state.balls.size); assertEquals(0,e.state.energy); assertEquals(20000,e.state.novaRemainingMs)
        run(e,1000); assertTrue(e.state.novaRemainingMs in 18990..19000); assertTrue(e.state.nudgeCooldownMs in 1990..2000)
    }
    @Test fun saveRoundTripIsExactWithMultiballChargeAndContactsAndInputsClear() {
        val e=PinballEngine(123); e.startCharge(); run(e,560); e.releaseCharge(); e.setFlippers(true,true); run(e,2340)
        val restored=PinballEngine.restore(e.save())!!; assertEquals(e.state,restored.state)
        e.clearInputs(); repeat(300) { e.advance(16);restored.advance(16); assertEquals(e.state,restored.state) }
        assertEquals(e.save(),restored.save())
        assertNull(PinballEngine.restore("broken")); assertNull(PinballEngine.restore(e.save().replaceFirst("PB2","PB9")))
    }
    @Test fun fixedStepsAgreeAcrossFrameRatesAndLargeBacklogIsBounded() {
        val a=PinballEngine(4); val b=PinballEngine(4); a.releaseCharge();b.releaseCharge()
        repeat(100) { a.advance(16) };repeat(200) { b.advance(8) };assertEquals(a.state,b.state)
        val before=a.state.elapsedMs;a.advance(Long.MAX_VALUE);assertTrue(a.state.elapsedMs-before<=100)
    }
    @Test fun tracksMatchMouthsAndExitAboveFlippers() {
        assertEquals(185.0,PinballTable.trackPoint(1,0.0).x,0.01)
        assertEquals(815.0,PinballTable.trackPoint(2,0.0).x,0.01)
        for(i in 0..2) for(j in 0..100) { val p=PinballTable.trackPoint(i,j/100.0);assertTrue(p.x in 0.0..1000.0 && p.y in 0.0..1800.0 && p.z>=0) }
        assertTrue(PinballTable.trackPoint(1,1.0).y<1400)
    }
    private fun put(e: PinballEngine, s: PinballState) { e.javaClass.getDeclaredField("state").apply { isAccessible=true }.set(e,s) }
    private fun hit(e: PinballEngine,c: PinballCircle,id: Int) {
        put(e,e.state.copy(balls=listOf(ball(c.x,c.y+c.radius+17,vy=-600.0,id=id)),awaitingLaunch=false)); e.advance(10)
    }
    @Test fun missionChainRequiresDistinctTargetsAndBothPhysicalRampsThenFiveCoreHits() {
        val e=PinballEngine(2)
        repeat(6){hit(e,PinballTable.bumpers[it%3],it+1)}
        assertEquals(PinballMission.TARGETS,e.state.mission)
        hit(e,PinballTable.targets[0],7);hit(e,PinballTable.targets[0],8);assertEquals(1,e.state.missionProgress)
        hit(e,PinballTable.targets[1],9);hit(e,PinballTable.targets[2],10)
        assertEquals(PinballMission.RAMPS,e.state.mission);assertEquals(2,e.state.multiplier)
        fun ramp(track: Int,id: Int){val p=PinballTable.trackPoint(track,0.0);put(e,e.state.copy(balls=listOf(ball(p.x,p.y+8,vy=-700.0,id=id))));e.advance(10);assertEquals(track,e.state.balls.single().track)}
        ramp(1,11);ramp(1,12);assertEquals(1,e.state.missionProgress);ramp(2,13)
        assertEquals(PinballMission.CORE,e.state.mission)
        repeat(5){hit(e,PinballTable.core,14+it)}
        assertEquals(PinballMission.BUMPERS,e.state.mission);assertEquals(2,e.state.sector);assertTrue(e.state.score>=30000)
    }
    @Test fun novaDoublesPhysicalJackpotsAndRetainsRemainingBallsAfterTimer() {
        val normal=fixture(PinballState(balls=listOf(ball(500.0,1104.0,vy=-600.0)),awaitingLaunch=false))
        normal.advance(10)
        val nova=fixture(PinballState(balls=listOf(ball(500.0,1104.0,vy=-600.0)),awaitingLaunch=false,novaRemainingMs=1000))
        nova.advance(10);assertEquals(10000,nova.state.score);assertTrue(nova.state.score>normal.state.score);assertEquals(0,nova.state.energy)
        val timed=fixture(PinballState(balls=listOf(ball(950.0,1640.0).copy(track=0),ball(950.0,1640.0,id=2).copy(track=0)),awaitingLaunch=false,novaRemainingMs=1))
        timed.advance(10);assertEquals(0,timed.state.novaRemainingMs);assertEquals(2,timed.state.balls.size)
    }
    @Test fun normalCoreHitIsAMissionContactAndJackpotCelebrationRequiresNova() {
        val e=fixture(PinballState(balls=listOf(ball(500.0,1104.0,vy=-600.0)),awaitingLaunch=false,mission=PinballMission.CORE,missionGoal=5))
        e.advance(10);assertEquals(1,e.state.missionProgress);assertFalse(e.drainEvents().any { it.kind==PinballEventKind.JACKPOT })
    }
    @Test fun novaDoublesMissionBonusesAsWellAsContactPoints() {
        fun finish(boost: Boolean): PinballEngine {
            val e=fixture(PinballState(balls=listOf(ball(500.0,1104.0,vy=-600.0)),awaitingLaunch=false,mission=PinballMission.CORE,missionProgress=4,missionGoal=5,novaRemainingMs=if(boost)1000 else 0))
            e.advance(10);return e
        }
        val normal=finish(false);val boost=finish(true)
        assertEquals(2*normal.drainEvents().single { it.kind==PinballEventKind.MISSION }.points,boost.drainEvents().single { it.kind==PinballEventKind.MISSION }.points)
    }
    @Test fun releasingAFlipperDoesNotLaunchAndRestingBumperOverlapCannotScore() {
        val s=PinballTable.flipper(true,1.0);val x=s.ax+(s.bx-s.ax)*.65;val y=s.ay+(s.by-s.ay)*.65-28
        val e=fixture(PinballState(balls=listOf(ball(x,y,vy=100.0)),awaitingLaunch=false,leftFlipper=1.0))
        run(e,100);assertTrue(e.state.balls.single().vy > -100)
        val c=PinballTable.bumpers[0];val resting=fixture(PinballState(balls=listOf(ball(c.x,c.y+c.radius+17)),awaitingLaunch=false))
        resting.advance(10);assertEquals(0,resting.state.score);assertEquals(0,resting.state.missionProgress)
    }
    @Test fun chargedLaunchSkillSpotAndCorruptSaveHaveExplicitBehavior() {
        val e=PinballEngine(1);e.startCharge();run(e,850);assertTrue(e.state.charge in .65.. .88);e.releaseCharge()
        assertTrue(e.drainEvents().any{it.kind==PinballEventKind.SKILL});assertTrue(e.state.score>=2500)
        val raw=e.save();assertNotNull(PinballEngine.restore(raw));assertNull(PinballEngine.restore(raw.dropLast(1)+"9"));assertNull(PinballEngine.restore("PB2|"+"A".repeat(30000)+"|0"))
    }
    @Test fun launchChargeChangesTravelSpeedAndNudgePreservesSpeedBound() {
        val tap=PinballEngine(2);val full=PinballEngine(2);full.startCharge();run(full,1200);tap.releaseCharge();full.releaseCharge();tap.advance(100);full.advance(100)
        assertTrue("Charge must influence the real launch",full.state.balls.single().y<tap.state.balls.single().y)
        val e=fixture(PinballState(balls=listOf(ball(500.0,1200.0,vx=1100.0,vy=-1600.0)),awaitingLaunch=false))
        e.nudge();assertTrue(hypot(e.state.balls.single().vx,e.state.balls.single().vy)<=2000.01)
    }
    @Test fun outlaneCannotEscapeTheVisibleTableBelowReturnRails() {
        val e=fixture(PinballState(balls=listOf(ball(81.0,1700.0,vx=-800.0,vy=100.0)),awaitingLaunch=false))
        e.advance(20);assertTrue(e.state.balls.single().x>=80)
    }
    @Test fun cancelAndRestoreClearHeldChargeWithoutLaunching() {
        val e=PinballEngine(9);e.startCharge();run(e,500);assertTrue(e.state.charge>0)
        val restored=PinballEngine.restore(e.save())!!;assertEquals(0.0,restored.state.charge,0.0);assertTrue(restored.state.awaitingLaunch)
        e.clearInputs();assertEquals(0.0,e.state.charge,0.0);assertTrue(e.state.balls.isEmpty());e.advance(100);assertEquals(0.0,e.state.charge,0.0)
    }
    @Test fun malformedGuidedSpeedIsRejectedEvenWithValidChecksum() {
        val e=PinballEngine(1);e.releaseCharge();val parts=e.save().split('|')
        val bytes=java.util.Base64.getDecoder().decode(parts[1]);java.nio.ByteBuffer.wrap(bytes).putDouble(182,0.0)
        val raw="PB2|${java.util.Base64.getEncoder().encodeToString(bytes)}|${java.util.zip.CRC32().apply{update(bytes)}.value}"
        assertNull(PinballEngine.restore(raw))
    }
    @Test fun multiballRestoreHasExactlyTheSameFutureContactsRulesAndTimers() {
        val e=PinballEngine(5);e.releaseCharge();repeat(2000) { frame ->
            e.setFlippers(frame%14<7,(frame+5)%17<8);if(e.state.awaitingLaunch)e.releaseCharge();e.advance(16)
            if(e.state.energy==100)e.activateNova()
        }
        assertTrue(e.state.balls.isNotEmpty());assertTrue(e.state.balls.size>1)
        e.clearInputs();e.drainEvents();val restored=PinballEngine.restore(e.save())!!
        repeat(1000) { frame ->
            val left=frame%17<8;val right=frame%13<7;e.setFlippers(left,right);restored.setFlippers(left,right)
            if(frame%200==0){assertEquals(e.nudge(),restored.nudge());assertEquals(e.activateNova(),restored.activateNova())}
            e.advance(16);restored.advance(16);assertEquals(e.state,restored.state);assertEquals(e.drainEvents(),restored.drainEvents())
        }
        assertEquals(e.save(),restored.save())
    }
    @Test fun variedPhysicalPlayCanReachEveryScoringFeatureWithoutFixtureProgress() {
        val reached=mutableSetOf<String>();var totalMs=0L;var firstEnergy=Long.MAX_VALUE;var novaRounds=0;var missionRounds=0;var maxScore=0;var maxDuration=0L;var bossRounds=0;val missions=IntArray(4);var completeRounds=0;val energyTimes=mutableListOf<Long>()
        repeat(80) { round ->
            val random=java.util.Random(round.toLong());val e=PinballEngine(round.toLong());var frames=0;var gotNova=false
            while(!e.state.gameOver&&frames<11250) {
                if(e.state.awaitingLaunch)e.releaseCharge()
                val s=e.state
                val threshold=1370+random.nextInt(170)
                val left=s.balls.any { it.track<0&&it.vy>0&&it.y>threshold&&it.x<550 }
                val right=s.balls.any { it.track<0&&it.vy>0&&it.y>threshold&&it.x>450 }
                e.setFlippers(if(round%3==0)frames%(8+round%11)<4 else left,if(round%3==0)(frames+3)%(9+round%13)<4 else right)
                if(s.energy==100&&!gotNova){firstEnergy=min(firstEnergy,s.elapsedMs);gotNova=e.activateNova();if(gotNova){novaRounds++;energyTimes+=s.elapsedMs}}
                if(s.nudgeCooldownMs==0L&&s.balls.any { it.y>1600&&it.track<0 })e.nudge()
                e.advance(16);frames++
                for(event in e.drainEvents()) {
                    when(event.kind) {
                        PinballEventKind.BUMPER->reached+="b"+PinballTable.bumpers.minBy { hypot(it.x-event.x,it.y-event.y) }.id
                        PinballEventKind.TARGET->{val c=PinballTable.targets.minBy { hypot(it.x-event.x,it.y-event.y) };if(hypot(c.x-event.x,c.y-event.y)<70)reached+="t${c.id}"}
                        PinballEventKind.RAMP->reached+=if(event.x<500)"r1" else "r2"
                        PinballEventKind.JACKPOT->{if(event.y in 980.0..1120.0)reached+="core"else if(event.y in 880.0..1030.0)reached+=if(event.x<500)"r1"else "r2"}
                        else->Unit
                    }
                }
                assertTrue(e.state.balls.all { it.x.isFinite()&&it.y.isFinite()&&it.vx.isFinite()&&it.vy.isFinite()&&hypot(it.vx,it.vy)<=2000.01 })
                if(frames%500==0){val restored=PinballEngine.restore(e.save());assertNotNull("Natural save must restore at $round/$frames: ${e.state}",restored);assertEquals(e.state,restored!!.state)}
            }
            if(e.state.mission!=PinballMission.BUMPERS||e.state.sector>1)missionRounds++
            missions[e.state.mission.ordinal]++;if(e.state.sector>1)bossRounds++;if(e.state.gameOver)completeRounds++
            totalMs+=e.state.elapsedMs;maxDuration=max(maxDuration,e.state.elapsedMs);maxScore=max(maxScore,e.state.score)
        }
        println("PINBALL PLAY: features=$reached averageMs=${totalMs/80} maxMs=$maxDuration novaRounds=$novaRounds earliestEnergyMs=$firstEnergy missionRounds=$missionRounds maxScore=$maxScore missions=${missions.toList()} bossRounds=$bossRounds endedRounds=$completeRounds energyMedianMs=${energyTimes.sorted().getOrNull(energyTimes.size/2)}")
        assertTrue("All shots must be physically reachable: $reached",reached.containsAll(listOf("b0","b1","b2","t0","t1","t2","r1","r2","core")))
        assertTrue("Meaningful energy must be reachable in under a minute",firstEnergy<60000)
        assertTrue("Play must progress bumper mission",missionRounds>0)
        assertTrue("The whole mission chain including boss must be physically completable",bossRounds>0)
    }
}
