# Arcade expansion implementation plan

> **For agentic workers:** Use superpowers:subagent-driven-development for scoped implementation, with superpowers:dispatching-parallel-agents for the independent game modules below. Preserve exclusive file ownership. Parent integrates and verifies the complete application.

**Goal:** Deliver eight fully playable offline games beside the existing Tetris, excluding Sudoku.

**Architecture:** Platform-independent Kotlin engines own rules and versioned, validated save strings. Compose screens own presentation and input. A shared scaffold provides pause, restart, rules and home navigation; a shared store saves each game independently.

**Tech Stack:** Existing Kotlin 2.1.20, Compose, Android API 26–36 and JUnit 4. No new runtime dependencies.

**Spec:** User-approved expansion proposal in this conversation: 2048, Snake, Breakout, Minesweeper, Sokoban, Link pairs, Bubble shooter, plus Spider solitaire; no Sudoku. Existing Tetris and Release 0.2 audio remain intact.

## Global constraints

- Work in the requested current folder; preserve existing committed Release 0.2 audio and private signing files.
- Chinese copy, calm dark backgrounds, bright pieces, large input targets, no faces in controls.
- Gameplay occupies most of the screen. Animations never block input. Pause on focus loss/background and discard elapsed background time.
- Independent offline save, high score/progress, restart and readable rules for each game.
- Snake has classic accelerating and fixed speed options. 2048 and Sokoban have undo. Link pairs has no compulsory timer, hints and solvable reshuffles. Spider defaults to authentic single-suit 104-card rules with undo.
- No publishing or remote release is necessary for this implementation request; produce a signed installable local 0.3 APK.

## Shared interfaces (parent owns these files)

- `app/.../MiniGameStore.kt`: `class MiniGameStore(context: Context)`; `load(id: String): String?`, `save(id: String, value: String)`, `best(id: String): Int`, `record(id: String, score: Int)`. Bounded strings, corrupt saves handled by each engine's safe restore.
- `app/.../ui/games/MiniGameScaffold.kt`: `@Composable fun MiniGameScaffold(title: String, session: GameSession, status: String, help: String, onRestart: () -> Unit, content: @Composable ColumnScope.() -> Unit)`.
- Same file: `@Composable fun MiniAction(label: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit)`.
- `GameSession`: `openMiniGame(id: String)` sets `screen="mini:$id"` and resumes; existing `paused`, `pause()`, `resume()`, `home()`, `settings` shared. `miniFeedback(celebrate: Boolean = false)` observes global sound/haptic settings.
- Every screen: `@Composable fun <Name>Screen(session: GameSession, saves: MiniGameStore)` in package `com.aurora.arcade.ui.games`. Names: `Twenty48`, `Snake`, `Breakout`, `Minesweeper`, `Sokoban`, `LinkPairs`, `Bubble`, `Spider`. Store IDs: `2048`, `snake`, `breakout`, `minesweeper`, `sokoban`, `link`, `bubble`, `spider`.
- Content uses `Modifier.weight(1f)` for its board; footer buttons >=48dp; all input guarded by `!session.paused`. Engines expose immutable state snapshots, safe save/restore. Save after discrete actions, periodically for continuous games, and on pause/disposal. Do not write every frame.
- Each group owns only its named engine/screen/test files. Parent alone runs repository Gradle commands to avoid shared build races. Groups can run standalone pure-Kotlin test harnesses with isolated output directories.

## Review focus

1. Rapid gestures and pause/resume must not create illegal extra moves or huge simulation jumps.
2. Corrupt/incompatible saves must safely start fresh, never crash or loop.
3. Game over/win supports restart and relevant undo; completed puzzles cannot mutate into invalid states.
4. Small phones, long Spider columns and 1.3x text remain usable.
5. Tetris behavior, sounds and records survive collection navigation.

## Task 1 — Shared shell and navigation (parent)
- [x] Implement the shared interfaces, lifecycle and nine-game home grid; retain the existing Tetris detail/mode chooser.
- [x] Add version 0.3/code 4, rules and progress labels; verify all nine routes on Android 16.

## Task 2 — Turn-based puzzles (puzzles agent)
- [x] Write rule tests for 2048 merge-once/no-op/undo, Minesweeper safe opening/flood/flags/win, Sokoban blocked push/undo/level completion and valid save round trips.
- [x] Implement `Twenty48Engine.kt`, `MinesweeperEngine.kt`, `SokobanEngine.kt` in `core/.../games/`, matching test files and Compose screens in `app/.../ui/games/`.
- [x] Provide true sliding/merging feedback, beginner/intermediate minefields, progressive verified Sokoban levels and big direction buttons.
- [x] Verify tests, save restore, pause guards and final screens; report exact coverage.

## Task 3 — Action games (action agent)
- [x] Write tests for Snake reversal/collision/food, Breakout collision/lives/clear and Bubble hex-neighbors/matches/detached groups; include bounded time and save validation.
- [x] Implement `SnakeEngine.kt`, `BreakoutEngine.kt`, `BubbleEngine.kt`, matching tests and screens.
- [x] Fixed simulation steps, buffered Snake directions, drag-following paddle, Bubble aiming/bank preview and launched ball animation; allow large touch controls.
- [x] Verify gameplay and report exact coverage; parent performs device performance checks.

## Task 4 — Cards and linking (cards agent)
- [x] Write tests for Spider 104-card deal/descending moves/deal restriction/K–A completion/undo/save, and Link pairs <=2-turn routes/hints/reshuffle/win/save.
- [x] Implement `SpiderEngine.kt`, `LinkPairsEngine.kt`, matching tests and screens.
- [x] Spider: 10 columns, 54 initial cards with only bottom card face-up, stock five rows of 10, no deal with empty columns, remove eight complete sequences to win. Mobile tap-select then tap-target, hints, undo, readable long columns. Single suit is default.
- [x] Link pairs: explicit same-type matching, outside-board routes, no timer pressure, hint/reshuffle, animated connection.
- [x] Verify rules and report exact coverage.

## Task 5 — Integration and delivery (parent)
- [x] Review every engine/screen, run `:core:test :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:assembleDebug` using cached toolchain.
- [x] Android 16: open/play/pause/save/reopen each game, test background and screen sizes; run Tetris regression and action-game frame metrics.
- [x] Verify signature and 16KB APK alignment. Save installable `releases/aurora-arcade-0.3.apk` and checksums.
- [x] Document controls, known limitations, measured verification and final progress. Do not imply physical-device measurements from emulator results.

## Progress ledger

- Baseline: clean `db17674` (Release 0.2). Eight new games authorized by user; no additional design approval needed.
- Interfaces fixed before dispatch. Independent group ownership avoids concurrent edits.

- Eight engines and screens complete; independent review found one action restart persistence ordering defect, fixed in all three screens. Final build currently passes 77 core + 7 audio tests and lint (0 errors). Android 16 navigation/background smoke passed all eight new games.

- Final: Android 16 gameplay, small-width/large-font and cold-process persistence checks passed. Tetris 120 drop/undo cycles passed (3,595 frames, modern jank 0.08%); Breakout modern jank 0.17%. Signed 0.3 APK and independent checksum delivered locally; no remote release created.
