package com.aurora.arcade

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aurora.arcade.ui.*

class MainActivity : ComponentActivity() {
    private lateinit var session: GameSession
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        session = GameSession(GameStore(applicationContext),Feedback(window.decorView))
        setContent {
            DisposableEffect(session.screen,session.paused,session.state.gameOver) {
                if(session.screen=="game" && !session.paused && !session.state.gameOver) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
            DisposableEffect(session.screen) {
                val controller=WindowCompat.getInsetsController(window,window.decorView)
                val previousBehavior=controller.systemBarsBehavior
                if(session.screen=="game") {
                    controller.systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
                onDispose {
                    controller.systemBarsBehavior=previousBehavior
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            AuroraTheme { ArcadeApp(session) }
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(!hasFocus && ::session.isInitialized) {
            session.clearInputs()
            if(session.screen == "game" && !session.paused && !session.state.gameOver) session.pause()
        }
    }
    override fun onPause() { if(::session.isInitialized) session.pause(); super.onPause() }
    override fun onDestroy() { if(::session.isInitialized) session.shutdown(); super.onDestroy() }
}
