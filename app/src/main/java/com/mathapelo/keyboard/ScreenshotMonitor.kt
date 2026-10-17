package com.mathapelo.keyboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Opt-in, local-only screenshot collection. Observes new MediaStore images
 * while the IME service process is alive and Android grants full Photos access.
 * It is NOT a global screenshot API and cannot guarantee capture after the OS
 * terminates the keyboard process or if media access is limited.
 */
class ScreenshotMonitor(
    private val context: Context,
    private val prefs: Prefs,
    private val onSaved: () -> Unit
) {
    private val worker=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private val scanning=AtomicBoolean(false)
    private var active=false
    private var lastSeen=0L
    private val observer=object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean) { scan() }
        override fun onChange(selfChange: Boolean, uri: Uri?) { scan() }
    }

    fun permissionGranted(): Boolean {
        val permission = if(Build.VERSION.SDK_INT>=33)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        return context.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED
    }

    fun start() {
        if(active)return
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,true,observer)
            active=true
        }catch(_:Exception){}
    }

    private fun scan() {
        if(Build.VERSION.SDK_INT<29 || !prefs.autoScreenshots || !permissionGranted() || !scanning.compareAndSet(false,true)) return
        worker.execute {
            try {
                val uri=MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val fields=arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.RELATIVE_PATH,MediaStore.Images.Media.DATE_ADDED)
                val now=System.currentTimeMillis()/1000L
                val results=mutableListOf<Pair<Long,Uri>>()
                var newest=lastSeen
                context.contentResolver.query(uri,fields,null,null,
                    MediaStore.Images.Media.DATE_ADDED+" DESC")?.use { cursor ->
                    val idCol=cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol=cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val pathCol=cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                    val dateCol=cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    var checked=0
                    while(cursor.moveToNext()&&checked++<15){
                        val id=cursor.getLong(idCol)
                        val name=cursor.getString(nameCol).orEmpty().lowercase(Locale.ROOT)
                        val folder=cursor.getString(pathCol).orEmpty().lowercase(Locale.ROOT)
                        val created=cursor.getLong(dateCol)
                        if(id>lastSeen&&created in (now-45)..(now+5) &&
                           (name.contains("screenshot")||folder.contains("screenshot"))) {
                            results.add(id to android.content.ContentUris.withAppendedId(uri,id))
                        }
                        if(id>newest)newest=id
                    }
                }
                lastSeen=newest
                // Read only screenshots added since monitoring; private app storage.
                var saved=false
                results.sortedBy { it.first }.forEach { (_,image) ->
                    if(ClipboardStore.saveImage(context,image,prefs))saved=true
                }
                if(saved)main.post(onSaved)
            }catch(_:SecurityException){}catch(_:Exception){}
            finally{scanning.set(false)}
        }
    }

    fun stop() {
        if(active)try{context.contentResolver.unregisterContentObserver(observer)}
        catch(_:Exception){}
        active=false
        worker.shutdown()
    }
}
