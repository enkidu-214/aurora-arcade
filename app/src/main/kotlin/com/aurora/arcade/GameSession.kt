package com.aurora.arcade

import androidx.compose.runtime.*
import com.aurora.arcade.core.*
import com.aurora.arcade.core.games.PinballEvent

class GameSession(private val store: GameStore,private val feedback: Feedback) {
    var engine = store.load() ?: GameEngine(System.nanoTime()); private set
    var state by mutableStateOf(engine.state); private set
    var screen by mutableStateOf("home"); private set
    var paused by mutableStateOf(true); private set
    var settings by mutableStateOf(store.settings()); private set
    var hasSaved by mutableStateOf(store.load() != null); private set
    var best by mutableIntStateOf(store.best(engine.mode)); private set
    var effect by mutableStateOf<GameEvent?>(null); private set
    var effectStart by mutableLongStateOf(0); private set
    var frameNanos by mutableLongStateOf(0); private set
    var canUndo by mutableStateOf(engine.canUndo); private set
    private var input = makeInput()
    private var lastFrame = 0L
    private var remainder = 0L
    private var saveElapsed = 0L
    private var lastEvent = 0L
    private fun makeInput() = RepeatController(if(settings.fastControls) 100 else 140,if(settings.fastControls) 28 else 40)
    fun newGame(mode: Mode) {
        feedback.stop()
        if(hasSaved) store.record(engine.mode,state.score)
        engine = GameEngine(System.nanoTime(),mode)
        state = engine.state; best = store.best(mode); canUndo = false
        effect = null; lastEvent = 0; input.clear(); lastFrame = 0; remainder = 0
        screen = "game"; paused = false; hasSaved = true; store.save(engine)
    }
    fun continueGame() { screen = "game"; resume() }
    fun openMiniGame(id: String) { pause(); screen = "mini:$id"; resume() }
    fun miniFeedback(celebrate: Boolean = false) {
        if (paused) return
        val piece = Piece(Kind.T)
        feedback.play(GameEvent(0, if (celebrate) EventType.CLEAR else EventType.ROTATE,
            piece, piece, rows = if (celebrate) listOf(0) else emptyList()), settings)
    }
    fun pinballFeedback(event: PinballEvent) { if (!paused) feedback.pinball(event, settings) }
    fun home() { pause(); store.record(engine.mode,engine.committedScore); best = store.best(engine.mode); screen = "home" }
    fun pause() { paused = true; input.clear(); feedback.stop(); lastFrame = 0; if(hasSaved) store.save(engine) }
    fun resume() { paused = false; input.clear(); lastFrame = 0; remainder = 0 }
    fun updateSettings(value: Settings) {
        if (!value.sound) feedback.stop()
        settings = value; store.settings(value); input.clear(); input = makeInput()
    }
    fun press(action: Action) {
        if(paused || screen != "game" || (state.gameOver && action != Action.UNDO)) return
        input.press(action).forEach(::command)
    }
    fun clearInputs() { input.clear() }
    fun release(action: Action) { input.release(action) }
    fun tap(action: Action) { press(action); release(action) }
    private fun command(action: Action) {
        if (paused || (state.gameOver && action != Action.UNDO)) return
        val wasGameOver = state.gameOver
        // Input takes priority over stale line-clear visuals.
        if(effect?.type == EventType.CLEAR) effect = null
        val changed = when(action) {
            Action.LEFT -> engine.move(-1)
            Action.RIGHT -> engine.move(1)
            Action.SOFT -> engine.softDrop()
            Action.CW -> engine.rotate(1)
            Action.CCW -> engine.rotate(-1)
            Action.DROP -> { engine.hardDrop(); true }
            Action.HOLD -> engine.hold()
            Action.UNDO -> engine.undo()
        }
        if(changed) {
            if(action == Action.UNDO && wasGameOver) { lastFrame = 0; remainder = 0 }
            publish()
            if(action == Action.LEFT || action == Action.RIGHT) feedback.play(null,settings)
        }
    }
    fun frame(nanos: Long) {
        if(paused || screen != "game") { lastFrame = 0; return }
        frameNanos = nanos
        if(lastFrame == 0L) { lastFrame = nanos; return }
        val delta = (nanos-lastFrame).coerceIn(0,50_000_000) + remainder
        lastFrame = nanos; remainder = delta % 1_000_000
        val ms = delta / 1_000_000
        input.advance(ms).forEach(::command)
        val clearing = effect?.type == EventType.CLEAR && nanos-effectStart < 180_000_000 && !settings.reducedMotion
        if(!clearing) engine.advance(ms)
        publish()
        saveElapsed += ms
        if(saveElapsed >= 2000) { saveElapsed = 0; store.save(engine) }
    }
    private fun publish() {
        state = engine.state; canUndo = engine.canUndo
        val newEvent = engine.event
        if(newEvent != null && newEvent.id != lastEvent) {
            lastEvent = newEvent.id
            // The frame loop may be asleep after game over; start undo effects at input time.
            effect = newEvent; effectStart = System.nanoTime()
            feedback.play(newEvent, settings, state.combo)
            if(newEvent.type in listOf(EventType.DROP,EventType.CLEAR,EventType.LOCK,EventType.UNDO,EventType.HOLD)) {
                store.save(engine)
                // A provisional hard-drop score is not committed to records until it survives undo.
                store.record(engine.mode,engine.committedScore)
                best = store.best(engine.mode)
            }
        }
        if(state.gameOver) input.clear()
    }
    fun shutdown() { pause(); feedback.close() }
}
