package com.mathapelo.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import android.content.ClipDescription
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * All clipboard persistence and screenshot imports live here.
 * An image screenshot is stored in private app storage with a preview, never uploaded.
 */
object ClipboardStore {
    private const val LIMIT = 30
    private const val MAX_IMAGE_BYTES = 10 * 1024 * 1024

    private fun directory(context: Context) = File(context.filesDir, "clips").also { it.mkdirs() }
    private fun file(context: Context, name: String) = File(directory(context), File(name).name)

    fun add(context: Context, value: String, prefs: Prefs) {
        val items = prefs.clips.toMutableList()
        items.remove(value)
        items.add(0, value)
        val pinned = prefs.clipPins.toSet()
        val kept = (items.filter { it in pinned } + items.filter { it !in pinned }).distinct().take(LIMIT)
        prefs.clips = kept
        prefs.clipPins = prefs.clipPins.filter { it in kept }
        val keepImages = kept.filter(::isImageClip).map(::imageNameOf).toSet()
        directory(context).listFiles()?.forEach { f -> if (f.name !in keepImages) f.delete() }
    }

    fun remove(context: Context, value: String, prefs: Prefs) {
        prefs.clips = prefs.clips.filterNot { it == value }
        prefs.clipPins = prefs.clipPins.filterNot { it == value }
        if (isImageClip(value)) file(context, imageNameOf(value)).delete()
    }

    fun togglePin(value: String, prefs: Prefs): Boolean {
        val pins = prefs.clipPins.toMutableList()
        val on = if (pins.contains(value)) { pins.remove(value); false } else { pins.add(value); true }
        prefs.clipPins = pins
        return on
    }

    fun entries(prefs: Prefs, query: String = ""): List<String> {
        val clips = prefs.clips
        val matches = if (query.isBlank()) clips else clips.filter {
            if (isImageClip(it)) "image screenshot picture photo".contains(query, ignoreCase = true)
            else it.contains(query, ignoreCase = true)
        }
        val pinned = prefs.clipPins.toSet()
        return matches.filter { it in pinned } + matches.filter { it !in pinned }
    }

    fun clear(context: Context, prefs: Prefs) {
        prefs.clips = emptyList()
        prefs.clipPins = emptyList()
        directory(context).deleteRecursively()
    }

    fun captureSystem(context: Context, clipboard: ClipboardManager, prefs: Prefs) {
        try {
            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount < 1) return
            val description = clip.description
            if (description.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true) return
            val item = clip.getItemAt(0)
            val uri = item.uri
            val isImage = (0 until description.mimeTypeCount).any {
                description.getMimeType(it).startsWith("image/")
            } || (uri != null && context.contentResolver.getType(uri)?.startsWith("image/") == true)
            if (isImage && uri != null) {
                saveImage(context, uri, prefs)
                return
            }
            if (description.hasMimeType("text/plain") || description.hasMimeType("text/html")) {
                val value = item.coerceToText(context)?.toString()?.trim().orEmpty()
                if (value.isNotEmpty() && value.length <= 4000) add(context, value, prefs)
            }
        } catch (_: Exception) { /* Some apps grant no clipboard URI permissions. */ }
    }

    fun saveImage(context: Context, uri: Uri, prefs: Prefs): Boolean {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val out = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val count = stream.read(chunk)
                    if (count < 0) break
                    if (out.size() + count > MAX_IMAGE_BYTES) return false
                    out.write(chunk, 0, count)
                }
                out.toByteArray()
            }
        } catch (_: Exception) { null } ?: return false
        if (bytes.size < 12) return false
        val ext = when {
            bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> "png"
            bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() -> "gif"
            bytes[0] == 'R'.code.toByte() && bytes[8] == 'W'.code.toByte() -> "webp"
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpg"
            else -> return false
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .take(16).joinToString("") { "%02x".format(it) }
        val name = "$hash.$ext"
        return try {
            val target = file(context, name)
            if (!target.exists()) target.writeBytes(bytes)
            add(context, IMG_PREFIX + name, prefs)
            true
        } catch (_: Exception) { false }
    }

    fun thumbnail(context: Context, name: String): Bitmap? {
        val target = file(context, name)
        if (!target.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.path, bounds)
            if (bounds.outWidth < 1 || bounds.outHeight < 1) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 360) sample *= 2
            BitmapFactory.decodeFile(target.path, BitmapFactory.Options().also { it.inSampleSize = sample })
        } catch (_: Exception) { null }
    }

    private fun mime(name: String) = when {
        name.endsWith(".png") -> "image/png"
        name.endsWith(".webp") -> "image/webp"
        name.endsWith(".gif") -> "image/gif"
        else -> "image/jpeg"
    }

    fun pasteImage(context: Context, name: String, ic: InputConnection?,
                   editor: EditorInfo?, clipboard: ClipboardManager): Boolean {
        val target = file(context, name)
        if (!target.exists()) {
            Toast.makeText(context, "Screenshot no longer available", Toast.LENGTH_SHORT).show()
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.clipfiles", target)
        if (ic != null && editor != null) {
            try {
                val content = InputContentInfoCompat(uri, ClipDescription("Screenshot", arrayOf(mime(name))), null)
                if (InputConnectionCompat.commitContent(
                        ic, editor, content,
                        InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null
                    )) return true
            } catch (_: Exception) {}
        }
        // Some messaging apps accept IME rich content; others only accept a clipboard image.
        return try {
            clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "Screenshot", uri))
            Toast.makeText(context, "Screenshot copied. Long-press and select Paste.", Toast.LENGTH_LONG).show()
            true
        } catch (_: Exception) {
            Toast.makeText(context, "This app can't accept screenshots", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
