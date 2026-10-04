package com.aurora.arcade

import android.content.Context

/** Small versioned game snapshots; apply() moves disk writes off the input/frame path. */
class MiniGameStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("arcade_games", Context.MODE_PRIVATE)

    fun load(id: String): String? = runCatching {
        preferences.getString("save_$id", null)?.takeIf { it.length <= MAX_SAVE_SIZE }
    }.getOrNull()

    fun save(id: String, value: String) {
        if (value.length <= MAX_SAVE_SIZE) preferences.edit().putString("save_$id", value).apply()
    }

    fun best(id: String): Int = runCatching { preferences.getInt("best_$id", 0).coerceAtLeast(0) }.getOrDefault(0)

    fun record(id: String, score: Int) {
        if (score > best(id)) preferences.edit().putInt("best_$id", score).apply()
    }

    companion object { private const val MAX_SAVE_SIZE = 128 * 1024 }
}
