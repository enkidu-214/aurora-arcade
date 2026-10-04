package com.aurora.arcade

import android.content.Context
import com.aurora.arcade.core.*
import java.io.File

class GameStore(context: Context) {
    private val prefs=context.getSharedPreferences("aurora",Context.MODE_PRIVATE)
    private val file=GameFileStore(File(context.filesDir,"session.bin"))
    fun load(): GameEngine? = file.load()?.let(GameEngine::restore)
    fun save(engine: GameEngine) { file.save(engine.exportSave()) }
    fun best(mode: Mode) = prefs.getInt("best_${mode.name}",0)
    fun record(mode: Mode,score: Int) { if(score>best(mode)) prefs.edit().putInt("best_${mode.name}",score).apply() }
    fun settings()=Settings(prefs.getBoolean("sound",true),prefs.getBoolean("haptic",true),prefs.getBoolean("reduced",false),prefs.getBoolean("fast",false))
    fun settings(s: Settings) { prefs.edit().putBoolean("sound",s.sound).putBoolean("haptic",s.haptic)
        .putBoolean("reduced",s.reducedMotion).putBoolean("fast",s.fastControls).apply() }
}
data class Settings(val sound: Boolean=true,val haptic: Boolean=true,val reducedMotion: Boolean=false,val fastControls: Boolean=false)
