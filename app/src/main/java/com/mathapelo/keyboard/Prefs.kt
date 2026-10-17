package com.mathapelo.keyboard

import android.content.Context
import android.util.Base64
import org.json.JSONArray

/** Live theme palette shared by the settings app and the IME process. */
object C {
    data class Palette(val bg: Int, val bar: Int, val key: Int, val pressed: Int,
                       val special: Int, val accent: Int, val accentDown: Int,
                       val text: Int, val dim: Int, val onAccent: Int)
    private val rose = Palette(0xFF1E1822.toInt(), 0xFF302636.toInt(),
        0xFF504056.toInt(), 0xFF705677.toInt(), 0xFF3B3041.toInt(),
        0xFFF5A7C6.toInt(), 0xFFE288B0.toInt(), 0xFFFFF8FC.toInt(),
        0xFFD9C3D2.toInt(), 0xFF311D2B.toInt())
    private val midnight = Palette(0xFF111822.toInt(), 0xFF202C3A.toInt(),
        0xFF314155.toInt(), 0xFF475C74.toInt(), 0xFF243345.toInt(),
        0xFF8FC8FF.toInt(), 0xFF65AFE8.toInt(), 0xFFF4F9FF.toInt(),
        0xFFB3C6DB.toInt(), 0xFF11243A.toInt())
    private val lavender = Palette(0xFF21192C.toInt(), 0xFF382A46.toInt(),
        0xFF5A476C.toInt(), 0xFF745C89.toInt(), 0xFF453451.toInt(),
        0xFFD7B0FF.toInt(), 0xFFBB91E4.toInt(), 0xFFFFF9FF.toInt(),
        0xFFE4CFEF.toInt(), 0xFF352041.toInt())
    private val pearl = Palette(0xFFF7F0F4.toInt(), 0xFFEFE2EA.toInt(),
        0xFFFFFFFF.toInt(), 0xFFE9D7E2.toInt(), 0xFFE9DBE5.toInt(),
        0xFFAA4778.toInt(), 0xFF923963.toInt(), 0xFF2C2230.toInt(),
        0xFF6C5D68.toInt(), 0xFFFFFFFF.toInt())
    val names = listOf("Rose", "Midnight", "Lavender", "Pearl")
    private var palette = rose
    fun use(name: String) { palette = when (name) {
        "Midnight" -> midnight
        "Lavender" -> lavender
        "Pearl" -> pearl
        else -> rose
    } }
    fun overrideColors(colors: Map<String, Int>) {
        if (colors.isEmpty()) return
        palette=palette.copy(
            bg=colors["background"] ?: palette.bg,
            bar=colors["bar"] ?: palette.bar,
            key=colors["key"] ?: palette.key,
            pressed=colors["pressed"] ?: palette.pressed,
            special=colors["special"] ?: palette.special,
            accent=colors["accent"] ?: palette.accent,
            accentDown=colors["accentDown"] ?: palette.accentDown,
            text=colors["text"] ?: palette.text,
            dim=colors["muted"] ?: palette.dim,
            onAccent=colors["onAccent"] ?: palette.onAccent
        )
    }
    val BG get() = palette.bg
    val BAR get() = palette.bar
    val KEY get() = palette.key
    val KEY_DOWN get() = palette.pressed
    val SPECIAL get() = palette.special
    val ACCENT get() = palette.accent
    val ACCENT_DOWN get() = palette.accentDown
    val TEXT get() = palette.text
    val TEXT_DIM get() = palette.dim
    val ACCENT_TEXT get() = palette.onAccent
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

    var autoScreenshots: Boolean
        get() = sp.getBoolean("autoScreenshots", false)
        set(v) { sp.edit().putBoolean("autoScreenshots", v).apply() }

    var theme: String
        get() = sp.getString("theme", "Rose") ?: "Rose"
        set(v) { sp.edit().putString("theme", v).apply() }

    var predictionsEnabled: Boolean
        get() = sp.getBoolean("predictionsEnabled", true)
        set(v) { sp.edit().putBoolean("predictionsEnabled", v).apply() }

    var autocorrectEnabled: Boolean
        get() = sp.getBoolean("autocorrectEnabled", true)
        set(v) { sp.edit().putBoolean("autocorrectEnabled", v).apply() }

    var customWords: String
        get() = sp.getString("customWords", "") ?: ""
        set(v) { sp.edit().putString("customWords", v).apply() }

    var learnedWords: List<String>
        get() = try {
            val array = JSONArray(sp.getString("learnedWords", "[]"))
            List(array.length()) { array.getString(it) }
        } catch (_: Exception) { emptyList() }
        set(v) { sp.edit().putString("learnedWords", JSONArray(v).toString()).apply() }

    fun learnWord(word: String) {
        val w = word.trim().lowercase(java.util.Locale.ROOT)
        if (w.length !in 3..24 || !w.all { it.isLetter() }) return
        val current = learnedWords.toMutableList()
        current.remove(w)
        current.add(0, w)
        learnedWords = current.take(250)
    }

    var clipPins: List<String>
        get() = try {
            val array = JSONArray(sp.getString("clipPins", "[]"))
            List(array.length()) { array.getString(it) }
        } catch (_: Exception) { emptyList() }
        set(v) { sp.edit().putString("clipPins", JSONArray(v).toString()).apply() }

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
