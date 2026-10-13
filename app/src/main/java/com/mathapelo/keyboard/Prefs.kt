package com.mathapelo.keyboard

import android.content.Context
import android.util.Base64
import org.json.JSONArray

object C {
    val BG = 0xFF241B29.toInt()
    val BAR = 0xFF33263A.toInt()
    val KEY = 0xFF5B4561.toInt()
    val KEY_DOWN = 0xFF795C80.toInt()
    val SPECIAL = 0xFF40304A.toInt()
    val ACCENT = 0xFFF2A6C1.toInt()
    val ACCENT_DOWN = 0xFFD783A5.toInt()
    val TEXT = 0xFFFFF7FC.toInt()
    val TEXT_DIM = 0xFFD4BDD0.toInt()
    val ACCENT_TEXT = 0xFF3B1F30.toInt()
}

class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("mathapelo_prefs", Context.MODE_PRIVATE)

    private val shortcutDefaults: Map<String, String> by lazy {
        try {
            val text = String(Base64.decode(BuildConfig.DEFAULT_DOT_SHORTCUTS_B64, Base64.DEFAULT), Charsets.UTF_8)
            text.lines().mapNotNull { line ->
                val split = line.indexOf('=')
                if (split <= 0) null else line.substring(0, split).trim() to line.substring(split + 1)
            }.toMap()
        } catch (_: IllegalArgumentException) {
            emptyMap()
        }
    }

    private fun defaultFor(trigger: String) = shortcutDefaults[trigger] ?: ""

    var dotId: String
        get() = sp.getString("dot_id", defaultFor(".id")) ?: ""
        set(v) { sp.edit().putString("dot_id", v).apply() }

    var dotNu: String
        get() = sp.getString("dot_nu", defaultFor(".nu")) ?: ""
        set(v) { sp.edit().putString("dot_nu", v).apply() }

    var dotEm: String
        get() = sp.getString("dot_em", defaultFor(".em")) ?: ""
        set(v) { sp.edit().putString("dot_em", v).apply() }

    var dotPas: String
        get() = sp.getString("dot_pas", defaultFor(".pas")) ?: ""
        set(v) { sp.edit().putString("dot_pas", v).apply() }

    var keyHeight: Int
        get() = sp.getInt("keyHeight", 46)
        set(v) { sp.edit().putInt("keyHeight", v).apply() }

    // 0 off, 1 light, 2 medium, 3 strong
    var vibration: Int
        get() = sp.getInt("vibration", 1)
        set(v) { sp.edit().putInt("vibration", v).apply() }

    var sound: Boolean
        get() = sp.getBoolean("sound", false)
        set(v) { sp.edit().putBoolean("sound", v).apply() }

    var autoCap: Boolean
        get() = sp.getBoolean("autoCap", true)
        set(v) { sp.edit().putBoolean("autoCap", v).apply() }

    var showNumberRow: Boolean
        get() = sp.getBoolean("showNumberRow", true)
        set(v) { sp.edit().putBoolean("showNumberRow", v).apply() }

    // 0 off, 1 left, 2 right
    var oneHanded: Int
        get() = sp.getInt("oneHanded", 0)
        set(v) { sp.edit().putInt("oneHanded", v).apply() }

    var shortcuts: String
        get() = sp.getString("shortcuts", "omw=on my way\nbrb=be right back\nidk=I don't know") ?: ""
        set(v) { sp.edit().putString("shortcuts", v).apply() }

    var clips: List<String>
        get() = try {
            val a = JSONArray(sp.getString("clips", "[]"))
            List(a.length()) { a.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
        set(v) { sp.edit().putString("clips", JSONArray(v).toString()).apply() }
}

// Image clips are stored in the clip list as IMG_PREFIX + file name.
const val IMG_PREFIX = "\u0001IMG:"

fun isImageClip(s: String) = s.startsWith(IMG_PREFIX)

fun imageNameOf(s: String) = s.removePrefix(IMG_PREFIX)

fun clearClipFiles(context: Context) {
    java.io.File(context.filesDir, "clips").deleteRecursively()
}
