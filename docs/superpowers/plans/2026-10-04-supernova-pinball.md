# Supernova Pinball Implementation Plan

> **For agentic workers:** Follow scoped subagent-driven development and parallel independent file ownership. User explicitly requested implementation; proceed with the concrete design below without another authorization gate.

**Goal:** Add an original, satisfying arcade pinball game as the collection's tenth game, with real flipper control, richer objectives, multiball and polished audiovisual feedback.

**Architecture:** Pure Kotlin collision/rules engine and validated save codec, shared world geometry, Compose Canvas projected table and native multi-touch controls. Dedicated original SoundPool effects. Existing lifecycle, offline store and settings remain in force.

**Tech stack:** Existing Kotlin/Compose, API 26–36, JUnit; no runtime dependency or third-party art/code required.

**Spec:** This document records the implementation-ready design derived from the user's request for a game like 3D Pinball, but more complex, satisfying and exciting. User preferences: large controls, smooth movement, a large game area, calm dark background, no faces on controls. Existing nine games must remain intact.

## Design and reference rationale

Original space/reactor theme, Chinese name **超新星弹球**, store ID `pinball`. A projected, shaded table has raised ramps, steel balls/shadows, bumpers, target banks, slingshots and two flippers. Classic three-ball rounds are supported by an eight-second ball-save window after launch, cooldown-limited nudging, clear mission arrows and escalating score opportunities.

Reference: the [Pinball FX Agents official-hosted guide](https://www.pinballfx.com/press/table_guides/Agents_Table_Guide_By_ShoryukenToTheChin.pdf) describes ramp/orbit shots, mission objectives, bonus multipliers, locks/multiball and jackpot scoring. [Demon's Tilt](https://www.demonstilt.com/) combines video pinball with bosses and multiball. [Zen's enhanced pinball presentation](https://zenstudios.com/games/pinball-fx-vr-indiana-jones-the-pinball-adventure/) uses added 3D effects and trails. Apply these gameplay principles to an original table and original generated sound effects.

Full 3D camera movement was considered, but a fixed projected table better preserves phone readability and immediate controls. Pure score-chasing was considered; linked objectives and a deliberate power activation provide more reasons to replay.

## Fixed rules

- World 1000 × 1800, ball radius 18. Physics fixed substeps <=1/240 second, bounded backlog and speed. Accurate moving capsule flippers; held flippers cannot repeatedly inject free launch impulses.
- Launch: hold/release a charge control (minimum viable launch even on a tap); sweet spot awards a skill-shot bonus. Launch lane leads to the upper playfield without instant drains.
- Three lives; one life spent only when the last active ball drains outside ball-save. Save and nova timers count active simulation time only.
- Missions: hit six bumpers → complete three distinct targets → complete two ramp/orbit shots → hit core five times. Completing the boss advances sector and grants a large reward. HUD always says what to aim for next.
- Targets and top lanes light banks for bonus multipliers (capped). Recent distinct scoring contacts form a timed combo; per-object/per-ball cooldowns prevent frame-by-frame score farming.
- Energy fills from meaningful hits. At 100, player can activate **超新星**: three balls, 20 seconds of double score and 8 seconds ball-save. At least two balls may remain after the boost expires. No new nova until energy re-earned after the current boost. Ramp/core shots award jackpots in nova.
- A nudge gives a bounded upward rescue impulse, 3-second cooldown. Held touch is never treated as repeated nudges.
- Slow/stuck-ball escape is conservative and does not manufacture score or missions.
- Local best and exact midgame state, including multiball, rules and collision cooldowns, survive restart. Held inputs clear on pause/restore. Invalid saves safely start a fresh game.

## Interfaces (frozen before parallel work)

Parent owns `PinballModel.kt` with immutable `PinballState`, `PinballBall`, `PinballMission`, `PinballEvent`, geometry records. Do not change existing fields/signatures; coordinate additions.

Engine agent provides `class PinballEngine(seed: Long = System.nanoTime())`:

- `val state: PinballState`
- `fun setFlippers(left: Boolean, right: Boolean)`; `fun clearInputs()`
- `fun startCharge()`; `fun releaseCharge(): Boolean`
- `fun nudge(): Boolean`; `fun activateNova(): Boolean`
- `fun advance(milliseconds: Long): Boolean`; `fun drainEvents(): List<PinballEvent>`
- `fun save(): String`; `companion object fun restore(raw: String?): PinballEngine?`

`object PinballTable` in `PinballTable.kt`:

- `const val WIDTH = 1000.0`, `HEIGHT = 1800.0`, `BALL_RADIUS = 18.0`
- `val walls: List<PinballSegment>`; `val bumpers: List<PinballCircle>` (IDs 0–2), `val targets: List<PinballCircle>` (IDs 0–2), `val core: PinballCircle`; `val slings: List<PinballSegment>`
- `fun flipper(left: Boolean, amount: Double): PinballSegment` (amount 0 down, 1 up)
- `fun trackPoint(track: Int, progress: Double): PinballPoint` for 0 launch, 1 left ramp, 2 right orbit. Renderer uses these exact paths for raised geometry.
- Geometry coordinates: flipper pivots (270,1530)/(730,1530), length 195, rest ±22° downward inward, lift 49°. Bumpers around upper middle, core centered above midfield; keep open shooting routes from flippers to ramp mouths and targets. Engine owns exact remaining layout and tells renderer promptly.

UI agent provides `@Composable fun PinballScreen(session: GameSession, saves: MiniGameStore)` and private renderer/control files. Shared `MiniGameScaffold`/`MiniAction`, `session.paused/settings/pause/resume/home`, and `saves.load/save/best/record` already exist. Parent adds `session.pinballFeedback(event: PinballEvent)`.

Sound agent provides `Feedback.pinball(event: PinballEvent, settings: Settings)` and integrates dedicated audio stop/close into Feedback. Own pure policy/output interface and preloaded SoundPool implementation; parent alone edits GameSession.

## Global constraints / review focus

- Preserve all existing uncommitted 0.3 work; work in user-requested current folder. No resets, bulk cleanup, commits or publishing needed. Version 0.4/code 5, same signing certificate.
- Draw/projection and collision geometry must agree; no balls visibly pass through flippers/rails or get stuck on guides.
- Both flippers work simultaneously; finger-up/cancel/focus loss clears held input. Control press reacts on DOWN, not click release.
- World substeps stable across 60/120 Hz; pause/resume does not advance physics, drain lives or spend timers.
- Animation never delays input. Honor sound/haptics/reduced-motion settings; no full-screen strobe or distracting camera shake.
- Save only after meaningful actions/periodically/pause/disposal, never every frame. Do not reproduce old-engine disposal overwrite fixed in 0.3.
- Native render/cache static geometry, bounded event/trail lists; avoid per-frame UI-tree recomposition and expensive effects.

## Tasks and ownership

### 1. Engine / rules / persistence
- [x] Tests first: flipper impulses/no held farming, wall/bumper contacts, lives/save windows, distinct missions, nova/cap/jackpots, nudging cooldown, timers, save roundtrip/corrupt input.
- [x] Own `core/.../games/PinballEngine.kt`, `PinballTable.kt`, optional `PinballSaveCodec.kt`; test files prefixed `Pinball`.
- [x] Simulate multiple seeded games and varied controls; prove physical shots can reach every target/ramps/core and progress missions. Tune before finalizing.

### 2. Table renderer / controls / screen
- [x] Own `app/.../ui/games/PinballScreen.kt`, `PinballCanvas.kt`, `PinballControls.kt`.
- [x] Large dual controls, independent multi-touch, launch charge + nova button, compact nudge control, readable score/objective/progress, game-over restart.
- [x] Depth rails/ramps, metallic rolling balls, bumper rings, trails, target lamps, energy pulse, mission/jackpot celebrations, reduced-motion fallback; frame drawing remains responsive.
- [x] Clear held input on pause and disposal. Save latest engine, reset frame timestamps on resume; sleep loops when ended/awaiting idle input.

### 3. Sound / haptics
- [x] Own dedicated pinball audio classes, `Feedback.kt` additions, original `pin_*.wav`, generator, audio unit tests.
- [x] Distinct flipper clack, bumper kick, sling, target tick, ramp whoosh, launch, nova buildup, jackpot chord, drain. Rate-limit low priority collisions, prioritize rewards.
- [x] Mute/pause/close stop all streams; unloaded audio is skipped rather than queued late.

### 4. Integration / verification (parent)
- [x] Add tenth home entry near top, original vector icon, route and session audio wrapper; update version/docs.
- [x] Build/test/lint all games, independent review of physics and lifecycle.
- [x] Android 16: real play, two-finger control, pause/background, exact midgame restore, launch/skill, nova/jackpot/mission fixtures, small screen and sustained frame measurements.
- [x] Deliver signed 0.4 APK/checksum and honest verification report with online references. No promise that fun or all physical-device frame rates can be proven by unit tests alone.

## Progress

- Existing 0.3 has 84 passing tests and verified Android 16 operation. Pinball scope designed; interfaces fixed before delegation.

- Completed: 115 tests, release build/lint, independent review, Android 16 multi-touch/pause/charge/restore/small-screen checks and a 61.97-second three-ball performance sample. Signed APK and checksum are in `releases/`; detailed evidence is in `docs/pinball-0.4-verification.md`.
