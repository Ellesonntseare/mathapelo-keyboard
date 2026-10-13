package com.mathapelo.keyboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

private const val OFF = 0
private const val ONCE = 1
private const val CAPS = 2

private const val PAGE_LETTERS = 0
private const val PAGE_SYM1 = 1
private const val PAGE_SYM2 = 2
private const val PAGE_EMOJI = 3
private const val PAGE_CLIP = 4
private const val PAGE_NUMPAD = 5

private const val MODE_TEXT = 0
private const val MODE_EMAIL = 1
private const val MODE_URL = 2
private const val MODE_NUMBER = 3
private const val MODE_PHONE = 4
private const val MODE_DATETIME = 5

private const val STYLE_NORMAL = 0
private const val STYLE_SPECIAL = 1
private const val STYLE_ACCENT = 2

private const val NUMBERS = "1234567890"

private val ALTS = mapOf(
    'a' to "àáâäæãåā", 'e' to "èéêëēėę", 'i' to "ìíîïī", 'o' to "òóôöœøō",
    'u' to "ùúûüū", 'c' to "çćč", 'n' to "ñń", 's' to "śšß", 'y' to "ÿý",
    'z' to "žźż", 'l' to "ł", 'd' to "đ"
)

private val EMOJI_TABS = listOf("😀", "👍", "🐶", "🍔", "⚽")

private val EMOJI = listOf(
    "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥳 😏 😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 😶 😐 😑 😬 🙄 😯 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕",
    "👍 👎 👌 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ ✋ 🤚 🖐️ 🖖 👋 🤝 🙏 👏 🙌 👐 🤲 💪 ✍️ ❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 🔥 ✨ ⭐ 🌟 💯 ✅ ❌",
    "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛 🦋 🐌 🐞 🐢 🐍 🦎 🐙 🦑 🦀 🐠 🐟 🐬 🐳 🦈 🐊 🐘 🦒 🦓 🐪 🌸 🌹 🌻 🌲 🌴 🍀",
    "🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🥑 🥦 🥕 🌽 🥔 🍞 🧀 🥚 🍳 🥞 🥓 🍔 🍟 🍕 🌭 🥪 🌮 🌯 🍝 🍜 🍲 🍣 🍱 🍤 🍙 🍚 🍦 🍩 🍪 🎂 🍰 🍫 🍬 🍭 🍿 ☕ 🍵 🥤 🍺 🍷",
    "⚽ 🏀 🏈 ⚾ 🎾 🏐 🏉 🎱 🏓 🏸 🥊 🎯 🎮 🎲 🎵 🎶 🎤 🎧 🎸 🎹 🎬 📱 💻 📷 💡 🔑 🔒 💰 🎁 🎉 🎈 🚗 🚕 🚌 ✈️ 🚀 ⏰ 📚 ✏️ 📌 🏠 🌍 🌙 ☀️ ⚡ ❄️ 🌈"
)

class MathapeloKeyboardService : InputMethodService() {

    private lateinit var prefs: Prefs
    private lateinit var clipboard: ClipboardManager
    private lateinit var audio: AudioManager
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var outer: FrameLayout
    private lateinit var root: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var bubbleView: TextView

    private val letterKeys = mutableListOf<Pair<TextView, Char>>()
    private var shiftKey: TextView? = null

    private var shift = OFF
    private var lastShiftTap = 0L
    private var page = PAGE_LETTERS
    private var fieldMode = MODE_TEXT
    private var isPassword = false
    private var emojiTab = 0
    private var recentKeys = ""

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { onClipChanged() }

    private val actionSet = setOf(
        EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_SEND,
        EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_PREVIOUS
    )

    // ---------- Lifecycle ----------

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        clipboard.addPrimaryClipChangedListener(clipListener)
    }

    override fun onDestroy() {
        clipboard.removePrimaryClipChangedListener(clipListener)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        outer = FrameLayout(this)
        outer.setBackgroundColor(C.BG)

        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(3), dp(4), dp(3), dp(6))

        topBar = LinearLayout(this)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44))

        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        root.addView(topBar)
        root.addView(body)
        outer.addView(root)

        bubbleView = TextView(this)
        bubbleView.gravity = Gravity.CENTER
        bubbleView.setTextColor(C.TEXT)
        bubbleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
        bubbleView.background = roundBg(C.KEY_DOWN)
        bubbleView.elevation = dp(6).toFloat()
        bubbleView.visibility = View.GONE
        outer.addView(bubbleView, FrameLayout.LayoutParams(dp(48), dp(52)))

        render()
        return outer
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val t = info?.inputType ?: 0
        val cls = t and InputType.TYPE_MASK_CLASS
        val v = t and InputType.TYPE_MASK_VARIATION
        val isText = cls == InputType.TYPE_CLASS_TEXT

        isPassword = isText && (v == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            v == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            v == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)

        fieldMode = when {
            cls == InputType.TYPE_CLASS_PHONE -> MODE_PHONE
            cls == InputType.TYPE_CLASS_NUMBER -> MODE_NUMBER
            cls == InputType.TYPE_CLASS_DATETIME -> MODE_DATETIME
            isText && (v == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                v == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) -> MODE_EMAIL
            isText && v == InputType.TYPE_TEXT_VARIATION_URI -> MODE_URL
            else -> MODE_TEXT
        }

        page = if (fieldMode == MODE_PHONE || fieldMode == MODE_NUMBER || fieldMode == MODE_DATETIME) PAGE_NUMPAD else PAGE_LETTERS
        shift = OFF
        recentKeys = ""
        if (::outer.isInitialized) render()
        autoCapitalize()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        handler.removeCallbacksAndMessages(null)
        recentKeys = ""
        hideBubble()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (newSelStart != newSelEnd || newSelEnd < oldSelEnd || newSelEnd > oldSelEnd + 1) {
            recentKeys = ""
        }
    }

    // ---------- Small helpers ----------

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun roundBg(color: Int): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.RECTANGLE
        g.cornerRadius = dp(9).toFloat()
        g.setColor(color)
        return g
    }

    private fun rowLayout(): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER
        r.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return r
    }

    private fun spacer(weight: Float): View {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(0, dp(1), weight)
        return v
    }

    private fun bodyHeight() = (if (prefs.showNumberRow) 5 else 4) * (dp(prefs.keyHeight) + dp(6))

    private fun feedback(sound: Int) {
        val ms = when (prefs.vibration) {
            1 -> 8L
            2 -> 16L
            3 -> 30L
            else -> 0L
        }
        if (ms > 0) {
            try {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (e: Exception) {
            }
        }
        if (prefs.sound) audio.playSoundEffect(sound)
    }

    // ---------- Key bubble ----------

    private fun showBubble(anchor: View, text: String) {
        if (text.isBlank()) return
        val a = IntArray(2)
        val o = IntArray(2)
        anchor.getLocationInWindow(a)
        outer.getLocationInWindow(o)
        val w = maxOf(anchor.width, dp(46))
        bubbleView.text = text
        val lp = bubbleView.layoutParams as FrameLayout.LayoutParams
        lp.width = w
        lp.height = dp(52)
        bubbleView.layoutParams = lp
        bubbleView.translationX = (a[0] - o[0] + (anchor.width - w) / 2).toFloat()
        bubbleView.translationY = maxOf(0, a[1] - o[1] - dp(46)).toFloat()
        bubbleView.visibility = View.VISIBLE
    }

    private fun hideBubble() {
        if (::bubbleView.isInitialized) bubbleView.visibility = View.GONE
    }

    // ---------- Building keys ----------

    private fun key(
        label: String,
        weight: Float,
        style: Int = STYLE_NORMAL,
        bubble: Boolean = false,
        alts: String = "",
        repeat: Boolean = false,
        sound: Int = AudioManager.FX_KEYPRESS_STANDARD,
        onTap: () -> Unit
    ): TextView {
        val rest = when (style) {
            STYLE_ACCENT -> C.ACCENT
            STYLE_SPECIAL -> C.SPECIAL
            else -> C.KEY
        }
        val down = if (style == STYLE_ACCENT) C.ACCENT_DOWN else C.KEY_DOWN

        val tv = TextView(this)
        tv.text = label
        tv.contentDescription = when (label) {
            "⌫" -> "Delete"
            "⇧" -> "Shift"
            "⏎" -> "Enter"
            "😊" -> "Emoji"
            else -> label
        }
        tv.gravity = Gravity.CENTER
        tv.setTextColor(if (style == STYLE_ACCENT) C.ACCENT_TEXT else C.TEXT)
        val size = when {
            label.length == 1 -> 20f
            label.length <= 4 -> 14f
            else -> 12f
        }
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        tv.background = roundBg(rest)
        tv.elevation = dp(1).toFloat()
        val lp = LinearLayout.LayoutParams(0, dp(prefs.keyHeight), weight)
        lp.setMargins(dp(3), dp(3), dp(3), dp(3))
        tv.layoutParams = lp

        var longFired = false
        var canceled = false
        val repeater = object : Runnable {
            override fun run() {
                onTap()
                handler.postDelayed(this, 45)
            }
        }
        val longPress = Runnable {
            if (repeat) {
                longFired = true
                repeater.run()
            } else if (alts.isNotEmpty()) {
                longFired = true
                hideBubble()
                showAlternates(alts)
            }
        }

        tv.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    longFired = false
                    canceled = false
                    v.background = roundBg(down)
                    feedback(sound)
                    if (bubble) showBubble(v, (v as TextView).text.toString())
                    handler.postDelayed(longPress, 380)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (e.x < 0 || e.x >= v.width || e.y < 0 || e.y >= v.height) {
                        canceled = true
                        handler.removeCallbacks(longPress)
                        handler.removeCallbacks(repeater)
                        v.background = roundBg(rest)
                        hideBubble()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    handler.removeCallbacks(repeater)
                    v.background = roundBg(if (v === shiftKey && shift != OFF) C.ACCENT else rest)
                    hideBubble()
                    if (e.actionMasked == MotionEvent.ACTION_UP && !longFired && !canceled) onTap()
                }
            }
            true
        }
        return tv
    }

    private fun barKey(label: String, onClick: () -> Unit): TextView {
        val tv = TextView(this)
        tv.text = label
        tv.contentDescription = when (label) {
            "✦" -> "Quick replacements"
            "Copied" -> "Copied text and pictures"
            "😊" -> "Emoji"
            "📋" -> "Clipboard history"
            "↔" -> "One-handed mode"
            "⚙" -> "Settings"
            "✕" -> "Close"
            else -> label
        }
        tv.gravity = Gravity.CENTER
        tv.setTextColor(C.TEXT)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label.length > 4) 14f else 20f)
        tv.background = roundBg(C.BAR)
        val lp = LinearLayout.LayoutParams(0, dp(38), 1f)
        lp.setMargins(dp(3), dp(3), dp(3), dp(3))
        tv.layoutParams = lp
        tv.setOnClickListener {
            feedback(AudioManager.FX_KEYPRESS_STANDARD)
            onClick()
        }
        return tv
    }

    private fun deleteKey(weight: Float = 1.5f) =
        key("⌫", weight, STYLE_SPECIAL, repeat = true, sound = AudioManager.FX_KEYPRESS_DELETE) {
            deleteOne()
        }

    private fun enterKey(weight: Float): TextView {
        val label = when (enterAction()) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "Search"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_DONE -> "Done"
            EditorInfo.IME_ACTION_PREVIOUS -> "Prev"
            else -> "⏎"
        }
        return key(label, weight, STYLE_ACCENT, sound = AudioManager.FX_KEYPRESS_RETURN) { pressEnter() }
    }

    private fun letterKey(c: Char): TextView {
        val k = key(c.toString(), 1f, STYLE_NORMAL, bubble = true, alts = ALTS[c] ?: "") { typeLetter(c) }
        letterKeys.add(k to c)
        return k
    }

    private fun letterRow(chars: String, padded: Boolean): LinearLayout {
        val r = rowLayout()
        if (padded) r.addView(spacer(0.5f))
        for (c in chars) r.addView(letterKey(c))
        if (padded) r.addView(spacer(0.5f))
        return r
    }

    private fun charRow(chars: String): LinearLayout {
        val r = rowLayout()
        val pad = (10 - chars.length) / 2f
        if (pad > 0f) r.addView(spacer(pad))
        for (c in chars) {
            val s = c.toString()
            r.addView(key(s, 1f, STYLE_NORMAL, bubble = true) { typeChar(s) })
        }
        if (pad > 0f) r.addView(spacer(pad))
        return r
    }

    // ---------- Rendering pages ----------

    private fun render() {
        if (!::outer.isInitialized) return
        applyOneHanded()
        buildTopBar()
        body.removeAllViews()
        letterKeys.clear()
        shiftKey = null
        when (page) {
            PAGE_LETTERS -> renderLetters()
            PAGE_SYM1 -> renderSymbols(1)
            PAGE_SYM2 -> renderSymbols(2)
            PAGE_EMOJI -> renderEmoji()
            PAGE_CLIP -> renderClipboard()
            PAGE_NUMPAD -> renderNumpad()
        }
        refreshLetters()
    }

    private fun applyOneHanded() {
        val mode = prefs.oneHanded
        val w = if (mode == 0) ViewGroup.LayoutParams.MATCH_PARENT
        else (resources.displayMetrics.widthPixels * 0.82f).toInt()
        val g = if (mode == 2) Gravity.END else Gravity.START
        root.layoutParams = FrameLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT, g)
    }

    private fun buildTopBar() {
        topBar.removeAllViews()
        val brand = TextView(this)
        brand.text = "Mathapelo"
        brand.setTextColor(C.ACCENT)
        brand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        brand.gravity = Gravity.CENTER_VERTICAL
        brand.setPadding(dp(10), 0, 0, 0)
        brand.layoutParams = LinearLayout.LayoutParams(0, dp(38), 1.35f)
        topBar.addView(brand)
        topBar.addView(barKey("Copied") { page = PAGE_CLIP; render() })
        topBar.addView(barKey("✦") { showQuickShortcuts() })
        topBar.addView(barKey("↔") {
            prefs.oneHanded = (prefs.oneHanded + 1) % 3
            applyOneHanded()
        })
        topBar.addView(barKey("⚙") { openSettings() })
    }

    private fun showQuickShortcuts() {
        topBar.removeAllViews()
        listOf(
            ".id" to prefs.dotId,
            ".nu" to prefs.dotNu,
            ".em" to prefs.dotEm,
            ".pas" to prefs.dotPas
        ).forEach { (trigger, value) ->
            topBar.addView(barKey(trigger) {
                if (value.isEmpty()) {
                    Toast.makeText(this, "Set $trigger in settings", Toast.LENGTH_SHORT).show()
                } else {
                    recentKeys = ""
                    currentInputConnection?.commitText(value, 1)
                    autoCapitalize()
                    buildTopBar()
                }
            })
        }
        topBar.addView(barKey("✕") { buildTopBar() })
    }

    private fun showAlternates(chars: String) {
        topBar.removeAllViews()
        for (ch in chars) {
            val s = if (shift != OFF) ch.uppercase() else ch.toString()
            topBar.addView(barKey(s) { pickAlt(s) })
        }
        topBar.addView(barKey("✕") { buildTopBar() })
    }

    private fun pickAlt(s: String) {
        commit(s)
        if (shift == ONCE) shift = OFF
        refreshLetters()
        buildTopBar()
    }

    private fun openSettings() {
        val i = Intent(this, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
        requestHideSelf(0)
    }

    private fun renderLetters() {
        if (prefs.showNumberRow) body.addView(charRow(NUMBERS))
        body.addView(letterRow("qwertyuiop", false))
        body.addView(letterRow("asdfghjkl", true))

        val r3 = rowLayout()
        val s = key("⇧", 1.5f, STYLE_SPECIAL) { onShiftTap() }
        shiftKey = s
        r3.addView(s)
        for (c in "zxcvbnm") r3.addView(letterKey(c))
        r3.addView(deleteKey())
        body.addView(r3)

        body.addView(bottomRow())
    }

    private fun renderSymbols(n: Int) {
        if (prefs.showNumberRow) body.addView(charRow(NUMBERS))
        if (n == 1) {
            body.addView(charRow("@#\$%&-+()/"))
            val r = rowLayout()
            r.addView(key("=\\<", 1.5f, STYLE_SPECIAL) { page = PAGE_SYM2; render() })
            for (c in "*\"':;!?") {
                val s = c.toString()
                r.addView(key(s, 1f, STYLE_NORMAL, bubble = true) { typeChar(s) })
            }
            r.addView(deleteKey())
            body.addView(r)
        } else {
            body.addView(charRow("[]{}#%^*+="))
            body.addView(charRow("_\\|~<>€£¥"))
            val r = rowLayout()
            r.addView(key("?123", 1.5f, STYLE_SPECIAL) { page = PAGE_SYM1; render() })
            for (c in "©®™°•¿¡") {
                val s = c.toString()
                r.addView(key(s, 1f, STYLE_NORMAL, bubble = true) { typeChar(s) })
            }
            r.addView(deleteKey())
            body.addView(r)
        }
        body.addView(bottomRow())
    }

    private fun bottomRow(): LinearLayout {
        val r = rowLayout()
        val onLetters = page == PAGE_LETTERS
        r.addView(key(if (onLetters) "?123" else "ABC", 1.5f, STYLE_SPECIAL) {
            page = if (onLetters) PAGE_SYM1 else PAGE_LETTERS
            render()
        })

        when (fieldMode) {
            MODE_EMAIL -> r.addView(key("@", 1f, STYLE_SPECIAL, bubble = true) { commit("@") })
            MODE_URL -> r.addView(key("/", 1f, STYLE_SPECIAL, bubble = true) { commit("/") })
            else -> r.addView(key("😊", 1f, STYLE_SPECIAL) { page = PAGE_EMOJI; render() })
        }

        r.addView(key(",", 1f, STYLE_SPECIAL, bubble = true, alts = ";:'\"") { typeChar(",") })

        val web = fieldMode == MODE_EMAIL || fieldMode == MODE_URL
        val space = key("space", if (web) 2.5f else 4f, STYLE_NORMAL, sound = AudioManager.FX_KEYPRESS_SPACEBAR) {
            typeChar(" ")
        }
        space.setTextColor(C.TEXT_DIM)
        space.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        r.addView(space)

        r.addView(key(".", 1f, STYLE_SPECIAL, bubble = true, alts = "…?!:;-_") { typeChar(".") })
        if (web) r.addView(key(".com", 1.5f, STYLE_SPECIAL) { commit(".com") })
        r.addView(enterKey(1.5f))
        return r
    }

    private fun renderNumpad() {
        val phone = fieldMode == MODE_PHONE
        val rows = if (phone) {
            listOf(
                listOf("1", "2", "3", "DEL"), listOf("4", "5", "6", "*"),
                listOf("7", "8", "9", "#"), listOf("+", "0", "-", "ENTER")
            )
        } else if (fieldMode == MODE_DATETIME) {
            listOf(
                listOf("1", "2", "3", "DEL"), listOf("4", "5", "6", "/"),
                listOf("7", "8", "9", ":"), listOf("-", "0", ".", "ENTER")
            )
        } else {
            val flags = currentInputEditorInfo?.inputType ?: 0
            val signed = flags and InputType.TYPE_NUMBER_FLAG_SIGNED != 0
            val decimal = flags and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0
            listOf(
                listOf("1", "2", "3", "DEL"), listOf("4", "5", "6", ""),
                listOf("7", "8", "9", ""),
                listOf(if (signed) "-" else "", "0", if (decimal) "." else "", "ENTER")
            )
        }
        for (row in rows) {
            val line = rowLayout()
            for (k in row) {
                val v = when (k) {
                    "" -> spacer(1f)
                    "DEL" -> deleteKey(1f)
                    "ENTER" -> enterKey(1f)
                    else -> key(
                        k, 1f,
                        if (k[0].isDigit()) STYLE_NORMAL else STYLE_SPECIAL,
                        bubble = true
                    ) { commit(k) }
                }
                line.addView(v)
            }
            body.addView(line)
        }
    }

    private fun renderEmoji() {
        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            bodyHeight() - (dp(prefs.keyHeight) + dp(6))
        )
        val grid = LinearLayout(this)
        grid.orientation = LinearLayout.VERTICAL

        val list = EMOJI[emojiTab].split(" ").filter { it.isNotBlank() }
        for (chunk in list.chunked(8)) {
            val row = rowLayout()
            for (i in 0 until 8) {
                if (i < chunk.size) row.addView(emojiCell(chunk[i])) else row.addView(spacer(1f))
            }
            grid.addView(row)
        }
        scroll.addView(grid)
        body.addView(scroll)

        val bar = rowLayout()
        bar.addView(key("ABC", 1.5f, STYLE_SPECIAL) { page = PAGE_LETTERS; render() })
        EMOJI_TABS.forEachIndexed { i, t ->
            bar.addView(key(t, 1f, if (i == emojiTab) STYLE_ACCENT else STYLE_SPECIAL) {
                emojiTab = i
                render()
            })
        }
        bar.addView(deleteKey())
        body.addView(bar)
    }

    private fun emojiCell(e: String): TextView {
        val tv = TextView(this)
        tv.text = e
        tv.gravity = Gravity.CENTER
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        tv.layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
        tv.setOnClickListener {
            feedback(AudioManager.FX_KEYPRESS_STANDARD)
            commit(e)
        }
        return tv
    }

    private fun renderClipboard() {
        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            bodyHeight() - (dp(prefs.keyHeight) + dp(6))
        )
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL

        val clips = prefs.clips
        val textCount = clips.count { !isImageClip(it) }
        val imageCount = clips.size - textCount
        val summary = TextView(this)
        summary.text = "Copied  •  $textCount texts  •  $imageCount pictures"
        summary.setTextColor(C.TEXT_DIM)
        summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        summary.setPadding(dp(12), dp(10), dp(12), dp(8))
        list.addView(summary)
        if (clips.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Nothing copied yet.\nText and images you copy show up here."
            empty.setTextColor(C.TEXT_DIM)
            empty.gravity = Gravity.CENTER
            empty.setPadding(dp(16), dp(40), dp(16), dp(16))
            list.addView(empty)
        }
        for (c in clips) {
            if (isImageClip(c)) {
                list.addView(imageClipView(imageNameOf(c)))
                continue
            }
            val item = TextView(this)
            item.text = c
            item.setTextColor(C.TEXT)
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            item.maxLines = 2
            item.ellipsize = TextUtils.TruncateAt.END
            item.setPadding(dp(12), dp(10), dp(12), dp(10))
            item.background = roundBg(C.KEY)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(dp(3), dp(3), dp(3), dp(3))
            item.layoutParams = lp
            item.setOnClickListener {
                feedback(AudioManager.FX_KEYPRESS_STANDARD)
                commit(c)
            }
            list.addView(item)
        }
        scroll.addView(list)
        body.addView(scroll)

        val bar = rowLayout()
        bar.addView(key("ABC", 1.5f, STYLE_SPECIAL) { page = PAGE_LETTERS; render() })
        bar.addView(key("Clear all", 2f, STYLE_SPECIAL) {
            prefs.clips = emptyList()
            clearClipFiles(this)
            render()
        })
        bar.addView(spacer(5f))
        body.addView(bar)
    }

    // ---------- Typing ----------

    private fun commit(text: String) {
        recentKeys = ""
        currentInputConnection?.commitText(text, 1)
        autoCapitalize()
    }

    private fun typeLetter(c: Char) {
        val out = if (shift != OFF) c.uppercaseChar() else c
        currentInputConnection?.commitText(out.toString(), 1)
        if (shift == ONCE) shift = OFF
        expandDotShortcut(out.toString())
        autoCapitalize()
        refreshLetters()
    }

    private fun typeChar(s: String) {
        val separator = s.length == 1 && s[0] in " .,!?;:"
        if (separator && expandShortcut(s)) {
            recentKeys = ""
            autoCapitalize()
            return
        }
        commit(s)
        expandDotShortcut(s)
        autoCapitalize()
    }

    private fun deleteOne() {
        recentKeys = ""
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        if (sel != null && sel.isNotEmpty()) {
            ic.commitText("", 1)
        } else {
            if (!ic.deleteSurroundingTextInCodePoints(1, 0)) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
        }
        autoCapitalize()
    }

    private fun enterAction(): Int {
        val info = currentInputEditorInfo ?: return EditorInfo.IME_ACTION_NONE
        if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return EditorInfo.IME_ACTION_NONE
        return info.imeOptions and EditorInfo.IME_MASK_ACTION
    }

    private fun pressEnter() {
        recentKeys = ""
        val action = enterAction()
        if (action in actionSet) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
        autoCapitalize()
    }

    // ---------- Text shortcuts ----------

    private fun expandDotShortcut(typed: String) {
        if (typed.length != 1) return
        val ic = currentInputConnection ?: return
        recentKeys = (recentKeys + typed).takeLast(8)
        val before = ic.getTextBeforeCursor(8, 0)?.toString()
        val suffix = if (isPassword || before.isNullOrEmpty()) recentKeys else before
        val match = listOf(
            ".pas" to prefs.dotPas,
            ".id" to prefs.dotId,
            ".nu" to prefs.dotNu,
            ".em" to prefs.dotEm
        ).firstOrNull { (trigger, value) -> value.isNotEmpty() && suffix.endsWith(trigger, ignoreCase = true) }
            ?: return

        ic.beginBatchEdit()
        if (ic.deleteSurroundingText(match.first.length, 0)) {
            ic.commitText(match.second, 1)
            recentKeys = ""
        }
        ic.endBatchEdit()
    }

    private fun expandShortcut(separator: String): Boolean {
        if (isPassword || fieldMode != MODE_TEXT) return false
        val ic = currentInputConnection ?: return false
        val before = ic.getTextBeforeCursor(40, 0)?.toString() ?: return false
        val word = before.takeLastWhile { it.isLetterOrDigit() || it == '\'' }
        if (word.isEmpty()) return false

        var expansion: String? = null
        for (line in prefs.shortcuts.lines()) {
            val i = line.indexOf('=')
            if (i > 0 && line.substring(0, i).trim().lowercase() == word.lowercase()) {
                val value = line.substring(i + 1).trim()
                if (value.isNotEmpty()) expansion = value
                break
            }
        }
        if (expansion == null) return false

        ic.beginBatchEdit()
        ic.deleteSurroundingText(word.length, 0)
        ic.commitText(expansion + separator, 1)
        ic.endBatchEdit()
        return true
    }

    // ---------- Shift, caps lock, auto-capitalize ----------

    private fun onShiftTap() {
        val now = SystemClock.uptimeMillis()
        shift = when {
            shift == CAPS -> OFF
            now - lastShiftTap < 400 -> CAPS
            shift == OFF -> ONCE
            else -> OFF
        }
        lastShiftTap = now
        refreshLetters()
    }

    private fun autoCapitalize() {
        if (!prefs.autoCap || isPassword || shift == CAPS) return
        val info = currentInputEditorInfo ?: return
        if (info.inputType == InputType.TYPE_NULL) return
        val mode = currentInputConnection?.getCursorCapsMode(info.inputType) ?: 0
        val wanted = if (mode != 0) ONCE else OFF
        if (shift != wanted) {
            shift = wanted
            refreshLetters()
        }
    }

    private fun refreshLetters() {
        for ((tv, c) in letterKeys) {
            tv.text = if (shift != OFF) c.uppercaseChar().toString() else c.toString()
        }
        shiftKey?.let {
            it.text = when (shift) {
                CAPS -> "⇪"
                ONCE -> "⬆"
                else -> "⇧"
            }
            it.background = roundBg(if (shift != OFF) C.ACCENT else C.SPECIAL)
            it.setTextColor(if (shift != OFF) C.ACCENT_TEXT else C.TEXT)
        }
    }

    // ---------- Clipboard history ----------

    private fun onClipChanged() {
        try {
            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount == 0) return
            val desc = clip.description
            if (desc.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true) return

            val item = clip.getItemAt(0)
            var hasImage = false
            for (i in 0 until desc.mimeTypeCount) {
                if (desc.getMimeType(i).startsWith("image/")) hasImage = true
            }
            val uri = item.uri
            val uriIsImage = if (uri != null) {
                try { contentResolver.getType(uri)?.startsWith("image/") == true }
                catch (_: Exception) { false }
            } else false
            if ((hasImage || uriIsImage) && uri != null) {
                saveImageClip(uri)
                return
            }

            val t = item.coerceToText(this)?.toString()?.trim() ?: return
            if (t.isEmpty() || t.length > 2000) return
            addClip(t)
        } catch (e: Exception) {
        }
    }

    private fun addClip(entry: String) {
        val list = prefs.clips.toMutableList()
        list.remove(entry)
        list.add(0, entry)
        val kept = list.take(20)
        prefs.clips = kept
        // delete image files that fell off the list
        val keepNames = kept.filter { isImageClip(it) }.map { imageNameOf(it) }.toSet()
        File(filesDir, "clips").listFiles()?.forEach {
            if (it.name !in keepNames) it.delete()
        }
        if (::outer.isInitialized && page == PAGE_CLIP) render()
    }

    private fun saveImageClip(uri: Uri) {
        val bytes = try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val output = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val count = stream.read(chunk)
                    if (count < 0) break
                    if (output.size() + count > 10 * 1024 * 1024) return
                    output.write(chunk, 0, count)
                }
                output.toByteArray()
            }
        } catch (e: Exception) {
            null
        }
        if (bytes == null || bytes.size < 12) return

        val hash = MessageDigest.getInstance("MD5").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val ext = when {
            bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> "png"
            bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() -> "gif"
            bytes[0] == 'R'.code.toByte() && bytes[8] == 'W'.code.toByte() -> "webp"
            else -> "jpg"
        }
        val dir = File(filesDir, "clips")
        dir.mkdirs()
        val name = "$hash.$ext"
        val file = File(dir, name)
        if (!file.exists()) file.writeBytes(bytes)
        addClip(IMG_PREFIX + name)
    }

    private fun mimeFor(name: String) = when {
        name.endsWith(".png") -> "image/png"
        name.endsWith(".gif") -> "image/gif"
        name.endsWith(".webp") -> "image/webp"
        else -> "image/jpeg"
    }

    private fun decodeThumb(file: File): Bitmap? {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 400) {
            sample *= 2
        }
        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        return BitmapFactory.decodeFile(file.path, opts)
    }

    private fun imageClipView(name: String): View {
        val file = File(File(filesDir, "clips"), name)
        val iv = ImageView(this)
        iv.contentDescription = "Copied picture"
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        iv.setPadding(dp(6), dp(6), dp(6), dp(6))
        iv.background = roundBg(C.KEY)
        val bmp = decodeThumb(file)
        if (bmp != null) iv.setImageBitmap(bmp)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120))
        lp.setMargins(dp(3), dp(3), dp(3), dp(3))
        iv.layoutParams = lp
        iv.setOnClickListener {
            feedback(AudioManager.FX_KEYPRESS_STANDARD)
            pasteImage(name)
        }
        return iv
    }

    private fun pasteImage(name: String) {
        val file = File(File(filesDir, "clips"), name)
        if (!file.exists()) {
            Toast.makeText(this, "Image is no longer available", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.clipfiles", file)
        val ic = currentInputConnection
        val info = currentInputEditorInfo

        var ok = false
        if (ic != null && info != null) {
            try {
                val content = InputContentInfoCompat(
                    uri, ClipDescription("image", arrayOf(mimeFor(name))), null
                )
                ok = InputConnectionCompat.commitContent(
                    ic, info, content,
                    InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null
                )
            } catch (e: Exception) {
                ok = false
            }
        }

        if (!ok) {
            // This app doesn't accept images from a keyboard: put it back on the
            // system clipboard so the person can long-press and tap Paste.
            try {
                clipboard.setPrimaryClip(ClipData.newUri(contentResolver, "image", uri))
                Toast.makeText(
                    this,
                    "This app can't take images from the keyboard. Image copied: long-press and tap Paste.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Couldn't paste image", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
