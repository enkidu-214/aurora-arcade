package com.aurora.arcade.core.games

import java.io.*
import java.util.Base64
import java.util.zip.CRC32
import kotlin.math.*

/** Deterministic fixed-step pinball. No timers use wall clock or coroutine lifetime. */
class PinballEngine(private val seed: Long = System.nanoTime()) {
    var state=PinballState(); private set
    private var leftHeld=false;private var rightHeld=false;private var charging=false
    private var pending=0L;private var ticks=0L;private var nextBall=1;private var nextEvent=1L
    private var missionMask=0;private var idleTicks=mutableMapOf<Int,Int>()
    private val cooldowns=mutableMapOf<String,Long>();private val contacts=mutableSetOf<String>()
    private val comboKeys=mutableSetOf<String>();private val events=ArrayDeque<PinballEvent>()
    private val dt=1.0/240.0
    fun setFlippers(left: Boolean,right: Boolean) { if(!state.gameOver){leftHeld=left;rightHeld=right} }
    fun clearInputs() { leftHeld=false;rightHeld=false;charging=false;if(state.charge!=0.0)state=state.copy(charge=0.0) }
    fun startCharge() { if(state.awaitingLaunch&&!state.gameOver)charging=true }
    fun releaseCharge(): Boolean {
        if(!state.awaitingLaunch||state.gameOver)return false
        val charge=state.charge;charging=false
        state=state.copy(balls=listOf(launchBall(charge)),awaitingLaunch=false,charge=0.0,ballSaveMs=8000)
        emit(PinballEventKind.LAUNCH,950.0,1640.0,label="发射")
        if(charge in .65.. .88) reward("skill",PinballEventKind.SKILL,2500,950.0,300.0,0,"精准发射")
        return true
    }
    private fun launchBall(charge: Double=.35): PinballBall { val p=PinballTable.trackPoint(0,0.0);return PinballBall(nextBall++,p.x,p.y,0.0,-(1300+400*charge),track=0) }
    fun nudge(): Boolean {
        if(state.gameOver||state.balls.isEmpty()||state.nudgeCooldownMs>0)return false
        state=state.copy(balls=state.balls.map {
            if(it.track<0) {
                val vy=(it.vy-420).coerceAtLeast(-2000.0);val vx=it.vx+(if(it.x<500)75 else -75);val scale=(2000/hypot(vx,vy)).coerceAtMost(1.0)
                it.copy(vx=vx*scale,vy=vy*scale)
            }else it
        },nudgeCooldownMs=3000)
        emit(PinballEventKind.NUDGE,500.0,1400.0,label="推台");return true
    }
    fun activateNova(): Boolean {
        if(state.gameOver||state.balls.isEmpty()||state.energy<100||state.novaRemainingMs>0)return false
        val balls=state.balls.toMutableList()
        while(balls.size<3) { val i=balls.size;balls+=PinballBall(nextBall++,if(i%2==0)620.0 else 380.0,420.0,if(i%2==0)-260.0 else 260.0,260.0) }
        state=state.copy(balls=balls,energy=0,novaRemainingMs=20000,ballSaveMs=max(state.ballSaveMs,8000))
        emit(PinballEventKind.MULTIBALL,500.0,650.0,label="超新星 · 三球爆发");return true
    }
    fun drainEvents(): List<PinballEvent> = events.toList().also { events.clear() }
    fun advance(milliseconds: Long): Boolean {
        if(milliseconds<=0||state.gameOver)return false
        if(state.balls.isEmpty()&&!charging&&state.leftFlipper==0.0&&state.rightFlipper==0.0&&!leftHeld&&!rightHeld)return false
        pending+=milliseconds.coerceAtMost(100)*240
        var changed=false
        while(pending>=1000) { pending-=1000;step();changed=true;if(state.gameOver){pending=0;break} }
        return changed
    }
    private fun step() {
        val previous=state
        val active=state.balls.isNotEmpty()
        val oldMs=ticks*1000/240;if(active)ticks++;val ms=ticks*1000/240-oldMs
        val left=(state.leftFlipper+(if(leftHeld)12 else -9)*dt).coerceIn(0.0,1.0)
        val right=(state.rightFlipper+(if(rightHeld)12 else -9)*dt).coerceIn(0.0,1.0)
        val comboTime=(state.comboRemainingMs-ms).coerceAtLeast(0)
        if(comboTime==0L)comboKeys.clear()
        state=state.copy(leftFlipper=left,rightFlipper=right,elapsedMs=state.elapsedMs+ms,
            charge=if(charging)(state.charge+dt*.85).coerceAtMost(1.0)else state.charge,
            comboRemainingMs=comboTime,combo=if(comboTime==0L)0 else state.combo,
            novaRemainingMs=(state.novaRemainingMs-ms).coerceAtLeast(0),ballSaveMs=(state.ballSaveMs-ms).coerceAtLeast(0),nudgeCooldownMs=(state.nudgeCooldownMs-ms).coerceAtLeast(0))
        if(!active)return
        if(state.leftFlipper>previous.leftFlipper&&previous.leftFlipper==0.0)emit(PinballEventKind.FLIPPER,270.0,1530.0)
        if(state.rightFlipper>previous.rightFlipper&&previous.rightFlipper==0.0)emit(PinballEventKind.FLIPPER,730.0,1530.0)
        val survivors=ArrayList<PinballBall>(3);var drained=false
        for(ball in previous.balls) {
            val b=if(ball.track>=0)guided(ball)else free(ball,previous)
            if(b.y>PinballTable.HEIGHT+PinballTable.BALL_RADIUS) {
                drained=true;idleTicks.remove(b.id);contacts.removeAll { it.startsWith("${b.id}:") }
                if(state.ballSaveMs>0) { survivors+=launchBall();emit(PinballEventKind.SAVE,b.x,1700.0,label="球已救回") }
                else emit(PinballEventKind.DRAIN,b.x,1700.0,label="落球")
            } else survivors+=b
        }
        state=state.copy(balls=survivors)
        if(drained&&survivors.isEmpty()) {
            val lives=state.lives-1
            state=state.copy(lives=lives,awaitingLaunch=lives>0,gameOver=lives==0,charge=0.0,novaRemainingMs=0,combo=0,comboRemainingMs=0)
            comboKeys.clear();contacts.clear();idleTicks.clear();clearInputs()
            if(lives==0)emit(PinballEventKind.GAME_OVER,500.0,1000.0,label="本局结束")
        }
        if(cooldowns.size>100)cooldowns.entries.removeAll { ticks-it.value>2400 }
    }
    private fun guided(ball: PinballBall): PinballBall {
        val duration=if(ball.track==0)1.35*1400/abs(ball.vy) else 1.9
        val p=ball.trackProgress+dt/duration;val position=PinballTable.trackPoint(ball.track,p)
        if(p>=1) {
            val vx=when(ball.track){0->-430.0*abs(ball.vy)/1400;1->-440.0;else->440.0}
            return ball.copy(x=position.x,y=position.y,z=0.0,vx=vx,vy=if(ball.track==0)280.0 else 310.0,track=-1,trackProgress=0.0)
        }
        return ball.copy(x=position.x,y=position.y,z=position.z,trackProgress=p)
    }
    private fun free(ball: PinballBall, old: PinballState): PinballBall {
        var x=ball.x+ball.vx*dt;var y=ball.y+ball.vy*dt;var vx=ball.vx*.9997;var vy=ball.vy+880*dt
        val touched=mutableSetOf<String>()
        fun segment(s: PinballSegment,radius: Double,restitution: Double,key: String?=null,surfaceX: Double=0.0,surfaceY: Double=0.0) {
            val dx=s.bx-s.ax;val dy=s.by-s.ay;val t=(((x-s.ax)*dx+(y-s.ay)*dy)/(dx*dx+dy*dy)).coerceIn(0.0,1.0)
            val px=s.ax+t*dx;val py=s.ay+t*dy;val dist=hypot(x-px,y-py);val total=18+radius
            if(dist>=total)return
            val nx=if(dist>1e-6)(x-px)/dist else -dy/hypot(dx,dy);val ny=if(dist>1e-6)(y-py)/dist else dx/hypot(dx,dy)
            x=px+nx*(total+.03);y=py+ny*(total+.03)
            val impact=(vx-surfaceX)*nx+(vy-surfaceY)*ny
            if(impact<0) { vx-=(1+restitution)*impact*nx;vy-=(1+restitution)*impact*ny }
            if(key!=null) {
                val k="${ball.id}:$key";touched+=k
                if(impact< -100&&contact(k,45)) { vx+=nx*280;vy+=ny*280;reward(key,PinballEventKind.SLING,150,x,y,2) }
            }
        }
        for(w in PinballTable.walls)segment(w,0.0,.82)
        for((i,s) in PinballTable.slings.withIndex())segment(s,8.0,.9,"s$i")
        for(left in listOf(true,false)) {
            val amount=if(left)state.leftFlipper else state.rightFlipper;val before=if(left)old.leftFlipper else old.rightFlipper
            val s=PinballTable.flipper(left,amount)
            val dx=s.bx-s.ax;val dy=s.by-s.ay;val t=(((x-s.ax)*dx+(y-s.ay)*dy)/(195*195)).coerceIn(0.0,1.0)
            val omega=Math.toRadians(-49.0)*(amount-before)/dt*(if(left)1 else -1)
            segment(s,12.0,.85,surfaceX=-omega*dy*t,surfaceY=omega*dx*t)
        }
        fun circle(c: PinballCircle,key: String,kind: PinballEventKind,base: Int,energy: Int,powered: Boolean=false) {
            val dx=x-c.x;val dy=y-c.y;val distance=hypot(dx,dy);val total=c.radius+18
            if(distance>=total+2)return
            val k="${ball.id}:$key";touched+=k
            if(distance>=total)return
            val nx=if(distance>1e-6)dx/distance else 0.0;val ny=if(distance>1e-6)dy/distance else -1.0
            x=c.x+nx*(total+.05);y=c.y+ny*(total+.05)
            val impact=vx*nx+vy*ny
            if(impact<0) { vx-=1.75*impact*nx;vy-=1.75*impact*ny }
            if(impact< -80&&contact(k,72)) {
                if(powered) { val outward=vx*nx+vy*ny;val kick=(820-outward).coerceAtLeast(0.0);vx+=nx*kick;vy+=ny*kick }
                reward(key,kind,base,x,y,energy,if(key=="core")if(state.novaRemainingMs>0)"核心大奖"else "核心命中"else "")
                when(kind) {
                    PinballEventKind.BUMPER->missionHit(PinballMission.BUMPERS,c.id)
                    PinballEventKind.TARGET->{ if(key=="core")missionHit(PinballMission.CORE,0)else { val mask=state.targetMask or (1 shl c.id);state=state.copy(targetMask=mask);if(mask==7){state=state.copy(targetMask=0,multiplier=(state.multiplier+1).coerceAtMost(5));emit(PinballEventKind.MISSION,x,y,label="目标组 · 倍率提升")};missionHit(PinballMission.TARGETS,c.id) } }
                    PinballEventKind.JACKPOT->missionHit(PinballMission.CORE,0)
                    else->Unit
                }
            }
        }
        for(c in PinballTable.bumpers)circle(c,"b${c.id}",PinballEventKind.BUMPER,200,5,true)
        for(c in PinballTable.targets)circle(c,"t${c.id}",PinballEventKind.TARGET,500,9)
        circle(PinballTable.core,"core",if(state.novaRemainingMs>0)PinballEventKind.JACKPOT else PinballEventKind.TARGET,if(state.novaRemainingMs>0)5000 else 800,10)
        // Mouth gates are directional: a descending return never re-enters a ramp.
        for(track in 1..2) {
            val p=PinballTable.trackPoint(track,0.0)
            if(vy< -220&&hypot(x-p.x,y-p.y)<65&&contact("${ball.id}:r$track",240)) {
                reward("r$track",if(state.novaRemainingMs>0)PinballEventKind.JACKPOT else PinballEventKind.RAMP,if(state.novaRemainingMs>0)5000 else 1200,x,y,15)
                missionHit(PinballMission.RAMPS,track-1)
                return ball.copy(x=p.x,y=p.y,vx=0.0,vy=0.0,z=0.0,track=track,trackProgress=0.0)
            }
        }
        // Top rollovers are unobstructed lamps rather than invisible collision shapes.
        if(ball.y>250&&y<=250) {
            val lane=when { x<420->0;x>580->2;else->1 };val k="${ball.id}:l$lane"
            if(contact(k,240)) { reward("l$lane",PinballEventKind.TARGET,350,x,y,3);val mask=state.laneMask or (1 shl lane);state=state.copy(laneMask=mask);if(mask==7)state=state.copy(laneMask=0,multiplier=(state.multiplier+1).coerceAtMost(5)) }
        }
        contacts.removeAll { it.startsWith("${ball.id}:")&&!touched.contains(it) };contacts+=touched
        val speed=hypot(vx,vy);if(speed>2000){vx*=2000/speed;vy*=2000/speed}
        val idle=if(speed<65&&y<1500)(idleTicks[ball.id]?:0)+1 else 0;idleTicks[ball.id]=idle
        if(idle>720){vy=210.0;vx=if(x<500)130.0 else -130.0;idleTicks[ball.id]=0}
        return ball.copy(x=x,y=y,vx=vx,vy=vy,z=0.0)
    }
    private fun contact(key: String,waitTicks: Long): Boolean {
        if(key in contacts||ticks-(cooldowns[key]?:-100000)<waitTicks)return false
        cooldowns[key]=ticks;return true
    }
    private fun reward(key: String,kind: PinballEventKind,base: Int,x: Double,y: Double,energy: Int,label: String="") {
        val distinct=comboKeys.add(key);val combo=comboKeys.size.coerceAtMost(6)
        val value=(base.toLong()*state.multiplier*(if(state.novaRemainingMs>0)2 else 1)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        state=state.copy(score=(state.score.toLong()+value).coerceAtMost(2_000_000_000).toInt(),energy=if(state.novaRemainingMs>0)0 else (state.energy+energy).coerceAtMost(100),combo=combo,comboRemainingMs=3000)
        emit(kind,x,y,value,label)
        if(distinct&&combo>=3) { val bonus=250*combo*(if(state.novaRemainingMs>0)2 else 1);state=state.copy(score=(state.score.toLong()+bonus).coerceAtMost(2_000_000_000).toInt());emit(PinballEventKind.COMBO,x,y,bonus,"${combo} 连击") }
    }
    private fun missionHit(mission: PinballMission,id: Int) {
        if(state.mission!=mission)return
        val distinct=mission==PinballMission.TARGETS||mission==PinballMission.RAMPS
        val bit=1 shl id;if(distinct&&missionMask and bit!=0)return
        if(distinct)missionMask=missionMask or bit
        val progress=state.missionProgress+1
        if(progress<state.missionGoal){state=state.copy(missionProgress=progress);return}
        val next=when(mission){PinballMission.BUMPERS->PinballMission.TARGETS;PinballMission.TARGETS->PinballMission.RAMPS;PinballMission.RAMPS->PinballMission.CORE;PinballMission.CORE->PinballMission.BUMPERS}
        val bonus=(if(mission==PinballMission.CORE)25000*state.sector else 3000)*(if(state.novaRemainingMs>0)2 else 1)
        state=state.copy(mission=next,missionProgress=0,missionGoal=goal(next),sector=if(mission==PinballMission.CORE)(state.sector+1).coerceAtMost(999)else state.sector,score=(state.score.toLong()+bonus).coerceAtMost(2_000_000_000).toInt())
        missionMask=0;emit(PinballEventKind.MISSION,500.0,900.0,bonus,if(mission==PinballMission.CORE)"核心击破 · 新星区"else "任务完成")
    }
    private fun emit(kind: PinballEventKind,x: Double,y: Double,points: Int=0,label: String="") {
        if(events.size>=64)events.removeFirst()
        events+=PinballEvent(nextEvent++,kind,x,y,state.elapsedMs,points,label)
    }
    fun save(): String {
        val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use { o ->
            o.writeLong(seed);o.writeLong(pending);o.writeLong(ticks);o.writeInt(nextBall);o.writeLong(nextEvent);o.writeInt(missionMask)
            val s=state
            listOf(s.score,s.lives,s.sector,s.energy,s.combo,s.multiplier,s.targetMask,s.laneMask,s.mission.ordinal,s.missionProgress,s.missionGoal).forEach(o::writeInt)
            o.writeBoolean(s.awaitingLaunch);o.writeBoolean(s.gameOver)
            listOf(s.leftFlipper,s.rightFlipper,s.charge).forEach(o::writeDouble)
            listOf(s.comboRemainingMs,s.novaRemainingMs,s.ballSaveMs,s.nudgeCooldownMs,s.elapsedMs).forEach(o::writeLong)
            o.writeInt(s.balls.size);for(b in s.balls){o.writeInt(b.id);listOf(b.x,b.y,b.vx,b.vy,b.z,b.trackProgress).forEach(o::writeDouble);o.writeInt(b.track)}
            o.writeInt(cooldowns.size);for((k,v)in cooldowns.toSortedMap()){o.writeUTF(k);o.writeLong(v)}
            for(set in listOf(contacts,comboKeys)){o.writeInt(set.size);set.sorted().forEach(o::writeUTF)}
            o.writeInt(idleTicks.size);for((k,v)in idleTicks.toSortedMap()){o.writeInt(k);o.writeInt(v)}
        }
        val payload=bytes.toByteArray();return "PB2|${Base64.getEncoder().encodeToString(payload)}|${CRC32().apply { update(payload) }.value}"
    }
    companion object {
        private fun goal(m: PinballMission)=when(m){PinballMission.BUMPERS->6;PinballMission.TARGETS->3;PinballMission.RAMPS->2;PinballMission.CORE->5}
        fun restore(raw: String?): PinballEngine? = runCatching {
            require(raw!=null&&raw.length<=30000)
            val parts=raw.split('|');require(parts.size==3&&parts[0]=="PB2")
            val bytes=Base64.getDecoder().decode(parts[1]);require(CRC32().apply { update(bytes) }.value==parts[2].toLong())
            val input=DataInputStream(ByteArrayInputStream(bytes));val e=PinballEngine(input.readLong())
            e.pending=input.readLong().also { require(it in 0..999) };e.ticks=input.readLong().also { require(it in 0..100_000_000_000L) }
            e.nextBall=input.readInt().also { require(it in 1..1_000_000) };e.nextEvent=input.readLong().also { require(it in 1..10_000_000_000L) };e.missionMask=input.readInt().also { require(it in 0..7) }
            val v=IntArray(11){input.readInt()};val awaiting=input.readBoolean();val over=input.readBoolean();val f=DoubleArray(3){input.readDouble()};val t=LongArray(5){input.readLong()}
            require(v[0] in 0..2_000_000_000&&v[1] in 0..3&&v[2] in 1..999&&v[3] in 0..100&&v[4] in 0..6&&v[5] in 1..5&&v[6] in 0..7&&v[7] in 0..7&&v[8] in 0..3)
            val mission=PinballMission.entries[v[8]];require(v[10]==goal(mission)&&v[9] in 0 until v[10])
            require(f.all { it.isFinite()&&it in 0.0..1.0 }&&t[0] in 0..3000&&t[1] in 0..20000&&t[2] in 0..8000&&t[3] in 0..3000&&t[4]==e.ticks*1000/240)
            val count=input.readInt();require(count in 0..3);val balls=List(count) {
                val id=input.readInt();val b=DoubleArray(6){input.readDouble()};val track=input.readInt()
                require(id in 1 until e.nextBall&&b.all { it.isFinite() }&&b[0] in -30.0..1030.0&&b[1] in 50.0..1818.0&&hypot(b[2],b[3])<=2100&&b[4] in 0.0..100.0&&b[5] in 0.0..1.0&&track in -1..2)
                val ball=PinballBall(id,b[0],b[1],b[2],b[3],b[4],track,b[5])
                if(track>=0){val p=PinballTable.trackPoint(track,b[5]);require(b[5]<1.0&&abs(p.x-b[0])<.01&&abs(p.y-b[1])<.01&&abs(p.z-b[4])<.01);require(b[2]==0.0&&(if(track==0)b[3] in -1700.0.. -1300.0 else b[3]==0.0))}else require(b[4]==0.0&&b[5]==0.0)
                ball
            }
            require(balls.map { it.id }.distinct().size==count&&over==(v[1]==0)&&awaiting==(count==0&&!over)&&(!over||count==0))
            if(mission==PinballMission.TARGETS||mission==PinballMission.RAMPS)require(Integer.bitCount(e.missionMask)==v[9]&&(mission!=PinballMission.RAMPS||e.missionMask<4))else require(e.missionMask==0)
            e.state=PinballState(balls,v[0],v[1],v[2],awaiting,over,f[0],f[1],f[2],v[3],v[4],t[0],v[5],v[6],v[7],mission,v[9],v[10],t[1],t[2],t[3],t[4])
            val coolCount=input.readInt();require(coolCount in 0..128);repeat(coolCount){val key=input.readUTF();val value=input.readLong();require(key.matches(Regex("[0-9]{1,7}:(b[0-2]|t[0-2]|s[0-1]|r[1-2]|l[0-2]|core)"))&&value in 0..e.ticks&&e.cooldowns.put(key,value)==null)}
            for(set in listOf(e.contacts,e.comboKeys)){val size=input.readInt();require(size in 0..48);repeat(size){val key=input.readUTF();require(key.length in 1..24&&set.add(key))}}
            require(e.contacts.all { key -> key.matches(Regex("[0-9]{1,7}:(b[0-2]|t[0-2]|s[0-1]|core)"))&&key.substringBefore(':').toInt() in balls.map { it.id } })
            require(e.comboKeys.all { it.matches(Regex("b[0-2]|t[0-2]|s[0-1]|r[1-2]|l[0-2]|core|skill")) }&&e.state.combo==e.comboKeys.size.coerceAtMost(6)&&((e.state.comboRemainingMs==0L)==e.comboKeys.isEmpty()))
            val idleCount=input.readInt();require(idleCount in 0..3);repeat(idleCount){val id=input.readInt();val idle=input.readInt();require(id in balls.map{it.id}&&idle in 0..720&&e.idleTicks.put(id,idle)==null)}
            require(input.available()==0);e.clearInputs();e
        }.getOrNull()
    }
}
