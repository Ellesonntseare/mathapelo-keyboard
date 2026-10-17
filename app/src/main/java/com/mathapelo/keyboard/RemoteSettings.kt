package com.mathapelo.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

/** Config-only HTTPS client. No text, photos, identifiers or clipboard content is uploaded. */
object RemoteSettings {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var lastFetch = 0L
    private var cacheRaw = ""
    private var cache: JSONObject? = null

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("mathapelo_remote", Context.MODE_PRIVATE)

    fun endpoint(context: Context): String = prefs(context).getString("endpoint", "") ?: ""
    fun configured(context: Context): Boolean =
        endpoint(context).startsWith("https://") && !prefs(context).getString("token", "").isNullOrBlank()

    fun storePairing(context: Context, url: String, token: String): Boolean {
        val normalized = url.trim().trimEnd('/')
        if (!isValidHttps(normalized) || token.trim().length !in 32..256) return false
        prefs(context).edit().putString("endpoint", normalized)
            .putString("token", token.trim()).apply()
        lastFetch = 0L
        return true
    }

    fun disconnect(context: Context) {
        prefs(context).edit().clear().apply()
        cacheRaw = ""
        cache = null
        lastFetch = 0L
    }

    private fun isValidHttps(url: String): Boolean = try {
        val u = URL(url)
        u.protocol == "https" && u.host.isNotBlank() &&
            u.userInfo == null && u.query == null && u.ref == null &&
            (u.path.isBlank() || u.path == "/")
    } catch (_: Exception) { false }

    fun revision(context: Context): Int = prefs(context).getInt("revision", 0)
    fun current(context: Context): JSONObject? {
        val raw = prefs(context).getString("config", "") ?: ""
        if (raw != cacheRaw) {
            cache = try { JSONObject(raw).takeIf { it.length() in 1..20 } } catch (_: Exception) { null }
            cacheRaw = raw
        }
        return cache
    }

    fun theme(context: Context, local: String): String =
        current(context)?.optString("theme", local)?.takeIf { it in C.names } ?: local
    fun height(context: Context, local: Int): Int =
        current(context)?.optInt("keyHeight", local)?.coerceIn(38,64) ?: local
    fun radius(context: Context): Int = current(context)?.optInt("keyRadius", 12)?.coerceIn(3,24) ?: 12
    fun animations(context: Context): Boolean = current(context)?.optBoolean("animations", true) ?: true
    fun predictions(context: Context, local: Boolean): Boolean =
        current(context)?.optBoolean("predictions", local) ?: local
    fun autocorrect(context: Context, local: Boolean): Boolean =
        current(context)?.optBoolean("autocorrect", local) ?: local
    fun numberRow(context: Context, local: Boolean): Boolean =
        current(context)?.optBoolean("numberRow", local) ?: local
    fun toolbar(context: Context): List<String> {
        val keys = listOf("clipboard","shortcuts","oneHanded","settings")
        val o = current(context) ?: return keys
        val a = o.optJSONArray("toolbar") ?: return keys
        return (0 until a.length()).mapNotNull { i ->
            a.optString(i).takeIf { it in keys }
        }.distinct()
    }
    fun dictionary(context: Context): List<String> {
        val a = current(context)?.optJSONArray("dictionary") ?: return emptyList()
        return (0 until minOf(a.length(),500)).mapNotNull { i ->
            a.optString(i).takeIf { it.length in 2..32 && it.all { ch ->
                ch.isLetter() || ch == '\'' || ch == '-'
            } }
        }
    }
    fun colors(context: Context): Map<String, Int> {
        val o = current(context)?.optJSONObject("colors") ?: return emptyMap()
        val allowed = listOf("background","bar","key","pressed","special",
            "accent","accentDown","text","muted","onAccent")
        return allowed.mapNotNull { name ->
            val v = o.optString(name,"")
            if (v.matches(Regex("#[0-9A-Fa-f]{6}"))) {
                name to (0xFF000000L or v.substring(1).toLong(16)).toInt()
            } else null
        }.toMap()
    }

    /**
     * Fetch only on a worker thread, with small bounded responses and no redirects.
     * A failure leaves the last known good version intact.
     */
    fun fetch(context: Context, force: Boolean = false, done: (Boolean, String) -> Unit = {_,_->}) {
        if (!configured(context)) {
            main.post { done(false, "Pair this keyboard with your PC first") }
            return
        }
        val now = android.os.SystemClock.elapsedRealtime()
        if (!force && now - lastFetch < 30000L) return
        lastFetch = now
        val app = context.applicationContext
        val base = endpoint(app)
        val token = prefs(app).getString("token","") ?: ""
        executor.execute {
            var connection: HttpsURLConnection? = null
            try {
                connection = (URL("$base/api/device/config").openConnection() as HttpsURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 3500
                    readTimeout = 3500
                    instanceFollowRedirects = false
                }
                if (connection.responseCode != 200) error("Server refused device connection")
                val bytes = connection.inputStream.use { stream ->
                    val data = java.io.ByteArrayOutputStream()
                    val buf = ByteArray(4096)
                    while (true) {
                        val read = stream.read(buf)
                        if (read < 0) break
                        if (data.size() + read > 32768) error("Config is too large")
                        data.write(buf,0,read)
                    }
                    data.toByteArray()
                }
                val body = JSONObject(String(bytes,Charsets.UTF_8))
                if (body.optInt("schema") != 1) error("Unsupported config schema")
                val remoteRevision = body.getInt("revision")
                val obj = body.getJSONObject("config")
                if (remoteRevision < 1 || remoteRevision < revision(app)) error("Outdated server configuration")
                if (obj.optString("theme") !in C.names) error("Invalid server theme")
                if (obj.length() > 20 || obj.optInt("keyHeight",46) !in 38..64) error("Invalid server settings")
                val changed = remoteRevision != revision(app)
                if (changed) {
                    prefs(app).edit().putString("config",obj.toString())
                        .putInt("revision",remoteRevision).apply()
                    cacheRaw = ""
                }
                main.post { done(changed, "Connected · Revision $remoteRevision") }
            } catch (e: Exception) {
                main.post { done(false, "Offline · using cached keyboard settings") }
            } finally {
                connection?.disconnect()
            }
        }
    }
}
