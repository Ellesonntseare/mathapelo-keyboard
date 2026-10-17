package com.mathapelo.keyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var column: LinearLayout
    private val importRequest = 91

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 18) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        C.use(prefs.theme)
        window.statusBarColor = C.BG
        window.navigationBarColor = C.BG

        column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(52))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(C.BG)
            isFillViewport = true
            addView(column)
        }
        setContentView(scroll)

        val eyebrow = label("YOUR PERSONAL KEYBOARD", 11f, C.ACCENT, 0, true)
        eyebrow.letterSpacing = .2f
        label("Mathapelo", 36f, C.TEXT, 4, true)
        label("A beautiful way to express yourself.", 15f, C.TEXT_DIM, 4)
        setup()
        sectionTitle("Make it yours", "Pick a signature look. Your keyboard updates the next time it opens.")
        themeChooser()
        sectionTitle("Live preview", "Your chosen theme, with softly rounded keys")
        preview()
        sectionTitle("Typing experience", "Fine-tune how the keyboard responds")
        typingCard()
        sectionTitle("Your dictionary", "Words unique to you, separated by commas or new lines")
        dictionaryCard()
        sectionTitle("Smart replacements", "Type these triggers to insert their saved text")
        shortcutCard()
        sectionTitle("Clipboard studio", "Pin, search and keep your copied text and screenshots")
        clipboardCard()
        label("Private by design  ·  Stored on this device  ·  No account required", 12f, C.TEXT_DIM, 26)
    }

    private fun label(value: String, size: Float, color: Int, margin: Int = 0, bold: Boolean = false): TextView {
        val view = TextView(this).apply {
            text = value
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(margin) }
        }
        column.addView(view)
        return view
    }

    private fun sectionTitle(title: String, subtitle: String) {
        label(title, 23f, C.TEXT, 28, true)
        label(subtitle, 13f, C.TEXT_DIM, 5)
    }

    private fun card(): LinearLayout {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(C.BAR, 20)
            setPadding(dp(15), dp(16), dp(15), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(13) }
        }
        column.addView(view)
        return view
    }

    private fun textIn(parent: LinearLayout, value: String, size: Float = 14f,
                       color: Int = C.TEXT, bold: Boolean = false): TextView {
        val t = TextView(this).apply {
            text = value
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }
        parent.addView(t)
        return t
    }

    private fun action(parent: LinearLayout, value: String, secondary: Boolean = false,
                       listener: () -> Unit) {
        val t = TextView(this).apply {
            text = value
            gravity = Gravity.CENTER
            setTextColor(if (secondary) C.TEXT else C.ACCENT_TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            background = rounded(if (secondary) C.SPECIAL else C.ACCENT, 13)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
            ).apply { topMargin = dp(10) }
            isClickable = true
            isFocusable = true
            setOnClickListener { listener() }
        }
        parent.addView(t)
    }

    private fun setup() {
        val box = card()
        textIn(box, "GET STARTED", 11f, C.ACCENT, true)
        textIn(box, "Two taps to make Mathapelo your keyboard", 16f, C.TEXT, true)
        action(box, "01  Enable in Android settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        action(box, "02  Choose Mathapelo", true) {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        textIn(box, "Try typing below", 13f, C.TEXT_DIM)
        field(box, "Type here to test your new keyboard", "", false) {}
    }

    private fun themeChooser() {
        val box = card()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        box.addView(row)
        C.names.forEachIndexed { index, name ->
            val themeColor = listOf(0xFFF5A7C6.toInt(), 0xFF8FC8FF.toInt(),
                0xFFD7B0FF.toInt(), 0xFFAA4778.toInt())[index]
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(10), dp(3), dp(9))
                background = rounded(if (name == prefs.theme) C.KEY_DOWN else C.SPECIAL, 13)
                layoutParams = LinearLayout.LayoutParams(0, dp(91), 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
                isClickable = true
                setOnClickListener {
                    prefs.theme = name
                    C.use(name)
                    recreate()
                }
            }
            val circle = View(this).apply {
                background = rounded(themeColor, 25)
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            }
            item.addView(circle)
            textIn(item, name, 11f, C.TEXT, name == prefs.theme).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(7), 0, 0)
            }
            row.addView(item)
        }
    }

    private fun preview() {
        val box = card()
        val words = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
        words.forEach { line ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            box.addView(row)
            line.forEach { ch ->
                row.addView(TextView(this).apply {
                    text = ch.toString()
                    gravity = Gravity.CENTER
                    setTextColor(C.TEXT)
                    textSize = 13f
                    background = rounded(C.KEY, 8)
                    layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                        setMargins(dp(2), dp(3), dp(2), dp(3))
                    }
                })
            }
        }
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        box.addView(bottom)
        listOf("?123", "😊", "Mathapelo space", "↵").forEachIndexed { i, key ->
            bottom.addView(TextView(this).apply {
                text = key
                gravity = Gravity.CENTER
                setTextColor(if (i == 3) C.ACCENT_TEXT else C.TEXT)
                textSize = 12f
                background = rounded(if (i == 3) C.ACCENT else C.KEY, 8)
                layoutParams = LinearLayout.LayoutParams(0, dp(38), if (i == 2) 3.5f else 1f)
                    .apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
            })
        }
    }

    private fun switchRow(parent: LinearLayout, title: String, hint: String,
                          enabled: Boolean, onChange: (Boolean) -> Unit) {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(9), 0, dp(9))
        }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        textIn(labels, title, 15f, C.TEXT, true)
        textIn(labels, hint, 12f, C.TEXT_DIM)
        r.addView(labels)
        r.addView(Switch(this).apply {
            isChecked = enabled
            setOnCheckedChangeListener { _, selected -> onChange(selected) }
        })
        parent.addView(r)
    }

    private fun typingCard() {
        val box = card()
        switchRow(box, "Word predictions", "Suggestions above the keys",
            prefs.predictionsEnabled) { prefs.predictionsEnabled = it }
        switchRow(box, "Autocorrect", "Fix common one-letter typos",
            prefs.autocorrectEnabled) { prefs.autocorrectEnabled = it }
        switchRow(box, "Auto capitalization", "Capitalize sentence starts", prefs.autoCap) {
            prefs.autoCap = it
        }
        switchRow(box, "Number row", "Show digits above letters", prefs.showNumberRow) {
            prefs.showNumberRow = it
        }
        switchRow(box, "Key sounds", "Play soft typing feedback", prefs.sound) {
            prefs.sound = it
        }
        val height = textIn(box, "Key height: ${prefs.keyHeight} dp", 13f, C.TEXT_DIM)
        box.addView(SeekBar(this).apply {
            max = 26
            progress = (prefs.keyHeight - 38).coerceIn(0, 26)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) { prefs.keyHeight = p + 38; height.text = "Key height: ${p + 38} dp" }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        val vibration = textIn(box, "Vibration: ${listOf("Off","Light","Medium","Strong")[prefs.vibration.coerceIn(0,3)]}", 13f, C.TEXT_DIM)
        box.addView(SeekBar(this).apply {
            max = 3
            progress = prefs.vibration.coerceIn(0,3)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) { prefs.vibration = p; vibration.text = "Vibration: ${listOf("Off","Light","Medium","Strong")[p]}" }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
    }

    private fun field(parent: LinearLayout, hint: String, value: String,
                      password: Boolean, onChange: (String) -> Unit): EditText {
        val f = EditText(this).apply {
            setText(value)
            this.hint = hint
            setTextColor(C.TEXT)
            setHintTextColor(C.TEXT_DIM)
            textSize = 14f
            isSingleLine = true
            background = rounded(C.SPECIAL, 11)
            setPadding(dp(13), dp(12), dp(13), dp(12))
            inputType = InputType.TYPE_CLASS_TEXT or
                (if (password) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_TEXT_VARIATION_NORMAL)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onChange(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        parent.addView(f)
        return f
    }

    private fun dictionaryCard() {
        val box = card()
        val words = EditText(this).apply {
            setText(prefs.customWords)
            hint = "e.g. Mathapelo, Mzansi, names and places"
            setTextColor(C.TEXT)
            setHintTextColor(C.TEXT_DIM)
            textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            background = rounded(C.SPECIAL, 10)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                    prefs.customWords = s?.toString().orEmpty()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        box.addView(words)
        textIn(box, "Learned words: ${prefs.learnedWords.size}  •  Kept on your device", 12f, C.TEXT_DIM)
        action(box, "Clear learned words", true) {
            prefs.learnedWords = emptyList()
            Toast.makeText(this, "Learned dictionary cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shortcutCard() {
        val box = card()
        listOf(".id", ".nu", ".em", ".pas").forEach { trigger ->
            textIn(box, trigger.uppercase(), 12f, C.ACCENT, true)
            val current = when (trigger) {
                ".id" -> prefs.dotId
                ".nu" -> prefs.dotNu
                ".em" -> prefs.dotEm
                else -> prefs.dotPas
            }
            field(box, "Value for $trigger", current, trigger == ".pas") { value ->
                when (trigger) {
                    ".id" -> prefs.dotId = value
                    ".nu" -> prefs.dotNu = value
                    ".em" -> prefs.dotEm = value
                    else -> prefs.dotPas = value
                }
            }
        }
        textIn(box, "Custom expansions (one per line: brb=be right back)", 13f, C.TEXT_DIM)
        val custom = EditText(this).apply {
            setText(prefs.shortcuts)
            setTextColor(C.TEXT)
            textSize = 14f
            background = rounded(C.SPECIAL, 10)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            minLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                    prefs.shortcuts = s?.toString().orEmpty()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        box.addView(custom)
    }

    private fun clipboardCard() {
        val box = card()
        textIn(box, "Screenshots and copied pictures appear with your text.", 13f, C.TEXT_DIM)
        action(box, "＋  Import a screenshot from Photos") {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, importRequest)
        }
        textIn(box, "Also use Photos → Share → Save to Mathapelo. Android doesn't always copy screenshots automatically.", 12f, C.TEXT_DIM)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val search = field(box, "Search saved clips or screenshots", "", false) {
            showClips(results, it)
        }
        box.addView(results)
        showClips(results, search.text.toString())
        action(box, "Delete all clipboard history", true) {
            ClipboardStore.clear(this, prefs)
            showClips(results, "")
            search.setText("")
            Toast.makeText(this, "Clipboard history cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showClips(parent: LinearLayout, query: String) {
        parent.removeAllViews()
        val matches = ClipboardStore.entries(prefs, query)
        if (matches.isEmpty()) { textIn(parent, "Nothing saved here yet", 13f, C.TEXT_DIM); return }
        matches.take(30).forEach { entry ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            val title = if (isImageClip(entry)) "▧  Screenshot / image" else entry
            row.addView(TextView(this).apply {
                text = title
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(C.TEXT)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            val pin = TextView(this).apply {
                text = if (entry in prefs.clipPins) "★" else "☆"
                setTextColor(C.ACCENT)
                textSize = 25f
                setPadding(dp(9), dp(5), dp(9), dp(5))
                setOnClickListener {
                    ClipboardStore.togglePin(entry, prefs)
                    showClips(parent, query)
                }
                contentDescription = "Pin or unpin"
            }
            row.addView(pin)
            row.addView(TextView(this).apply {
                text = "✕"
                setTextColor(C.TEXT_DIM)
                textSize = 18f
                setPadding(dp(9), dp(5), dp(4), dp(5))
                contentDescription = "Delete clip"
                setOnClickListener {
                    ClipboardStore.remove(this@MainActivity, entry, prefs)
                    showClips(parent, query)
                }
            })
            parent.addView(row)
        }
    }

    @Deprecated("Activity result used for broad API 26+ compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == importRequest && resultCode == RESULT_OK) {
            val success = data?.data?.let { ClipboardStore.saveImage(this, it, prefs) } == true
            Toast.makeText(this, if (success) "Screenshot saved to clipboard" else "Couldn't import image", Toast.LENGTH_SHORT).show()
            if (success) recreate()
        }
    }
}
