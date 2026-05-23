package com.focusguard.app

import android.content.Context
import android.content.SharedPreferences

/**
 * All persisted state. Read by both the UI and the accessibility service, so
 * every value is stored in a single SharedPreferences file.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("focusguard", Context.MODE_PRIVATE)

    var passwordHash: String?
        get() = sp.getString(KEY_HASH, null)
        set(v) = sp.edit().putString(KEY_HASH, v).apply()

    var salt: String?
        get() = sp.getString(KEY_SALT, null)
        set(v) = sp.edit().putString(KEY_SALT, v).apply()

    val hasPassword: Boolean get() = passwordHash != null && salt != null

    /** Wildcard URL patterns ("*" matches anything). */
    var patterns: List<String>
        get() = sp.getString(KEY_PATTERNS, null)
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: DEFAULT_PATTERNS
        set(v) = sp.edit().putString(KEY_PATTERNS, v.joinToString("\n")).apply()

    var blockYouTubeShorts: Boolean
        get() = sp.getBoolean(KEY_YT_SHORTS, true)
        set(v) = sp.edit().putBoolean(KEY_YT_SHORTS, v).apply()

    /** Minutes the user must wait, app open, before a settings unlock takes effect. */
    var cooldownMinutes: Int
        get() = sp.getInt(KEY_COOLDOWN, 5)
        set(v) = sp.edit().putInt(KEY_COOLDOWN, v).apply()

    /** Epoch millis until which protection is lifted (lets the user edit/uninstall). */
    var unlockedUntil: Long
        get() = sp.getLong(KEY_UNLOCKED_UNTIL, 0L)
        set(v) = sp.edit().putLong(KEY_UNLOCKED_UNTIL, v).apply()

    val isUnlocked: Boolean get() = System.currentTimeMillis() < unlockedUntil

    companion object {
        private const val KEY_HASH = "pw_hash"
        private const val KEY_SALT = "pw_salt"
        private const val KEY_PATTERNS = "patterns"
        private const val KEY_YT_SHORTS = "yt_shorts"
        private const val KEY_COOLDOWN = "cooldown_min"
        private const val KEY_UNLOCKED_UNTIL = "unlocked_until"

        val DEFAULT_PATTERNS = listOf(
            "*youtube.com/shorts*",
            "*://*.youtube.com/shorts*",
            "*facebook.com/reel*",
            "*instagram.com/reels*",
            "*tiktok.com*"
        )
    }
}
