package com.lnight.videoplayerproject.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat


fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else              -> null
}

fun Context.hideSystemUi() {
    val activity = this.findActivity() ?: return
    val window = activity.window ?: return
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).let { controller ->
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

fun Context.showSystemUi() {
    val activity = this.findActivity() ?: return
    val window = activity.window ?: return
    WindowCompat.setDecorFitsSystemWindows(window, true)
    WindowInsetsControllerCompat(
        window,
        window.decorView
    ).show(WindowInsetsCompat.Type.systemBars())
}

fun Context.getVideoPath(): List<Pair<String?, Uri?>> {
    val videoItemHashSet: MutableList<Pair<String?, Uri?>> = mutableListOf()
    val projection = arrayOf(
        MediaStore.Video.VideoColumns.DATA,
        MediaStore.Video.Media.DISPLAY_NAME
    )
    val cursor = this.contentResolver
        .query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Video.Media.DATE_TAKEN + " DESC")
    if(cursor != null) {
        try {
            cursor.moveToFirst()
            do {
                videoItemHashSet.add(Pair(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)).toUri().lastPathSegment, cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)).toUri()))
            } while (cursor.moveToNext())
            cursor.close()
        } catch (e: java.lang.Exception) {
           e.printStackTrace()
        }
    }
    return videoItemHashSet
}

fun Context.openAppDetails() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    startActivity(intent)
}