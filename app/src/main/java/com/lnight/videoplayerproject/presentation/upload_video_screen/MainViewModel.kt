package com.lnight.videoplayerproject.presentation.upload_video_screen

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.lnight.videoplayerproject.R
import com.lnight.videoplayerproject.presentation.MetadataReader
import com.lnight.videoplayerproject.presentation.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    val player: Player,
    private val metadataReader: MetadataReader,
    private val app: Application
): AndroidViewModel(app) {

    private val videoUris = MutableStateFlow<List<Triple<String?, Uri, ImageBitmap?>>>(emptyList())

    var videoItems by mutableStateOf<List<VideoItem>>(emptyList())
        private set

    init {
        player.prepare()
    }

    fun addVideoUri(urisWithData: List<Pair<String?, Uri?>>, singleMode: Boolean = false) {
        viewModelScope.launch {
            if(!singleMode) {
                videoUris.value = emptyList()
                videoItems = emptyList()
            }
            urisWithData.forEach {
                if(it.second != null) {
                val bitmap = getFrameAsBitmap(it.second!!)
                if (!videoUris.value.contains(Triple(it.first, it.second, bitmap))) {
                    videoUris.value = videoUris.value + Triple(it.first, it.second!!, bitmap)
                    val newValue = videoUris.value.map { uriWithData ->
                        VideoItem(
                            contentUri = uriWithData.second,
                            mediaItem = MediaItem.fromUri(uriWithData.second),
                            name = uriWithData.first
                                ?: metadataReader.getMetadataFromUri(uriWithData.second)?.fileName
                                ?: "No name",
                            videoFrame = uriWithData.third
                        )
                    }
                    videoItems = newValue
                    player.addMediaItem(MediaItem.fromUri(it.second!!))
                }
                videoUris.value.forEach { u ->
                    if (!File(u.second.toString()).exists()) {
                        videoUris.value = videoUris.value - Triple(u.first, u.second, u.third)
                        val newValue = videoUris.value.map { uriWithData ->
                            VideoItem(
                                contentUri = uriWithData.second,
                                mediaItem = MediaItem.fromUri(uriWithData.second),
                                name = uriWithData.first
                                    ?: metadataReader.getMetadataFromUri(uriWithData.second)?.fileName
                                    ?: "No name",
                                videoFrame = uriWithData.third
                            )
                        }
                        videoItems = newValue
                    }
                }
                }
            }
        }
    }

    fun playVideo(uri: Uri) {
        player.setMediaItem(
            videoItems.find { it.contentUri == uri }?.mediaItem ?: return
        )
    }

    private suspend fun getFrameAsBitmap(uri: Uri): ImageBitmap? {
        return try {
            val options = RequestOptions().frame(5000000L)
            val file = File(uri.toString())
            withContext(Dispatchers.IO) {
                Glide.with(app).asBitmap()
                    .load(file.absolutePath)
                    .apply(options)
                    .skipMemoryCache(false)
                    .placeholder(R.drawable.ic_loading)
                    .submit().get()
            }.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }

}