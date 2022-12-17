package com.lnight.videoplayerproject.presentation

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.media3.common.MediaItem

data class VideoItem(
    val contentUri: Uri,
    val mediaItem: MediaItem,
    val name: String,
    val videoFrame: ImageBitmap?
)
