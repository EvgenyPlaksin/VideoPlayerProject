package com.lnight.videoplayerproject.presentation.upload_video_screen

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.lnight.videoplayerproject.presentation.MetadataReader
import com.lnight.videoplayerproject.presentation.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val player: Player,
    private val metadataReader: MetadataReader
): ViewModel() {

    private val videoUris = MutableStateFlow<List<Pair<String?, Uri>>>(emptyList())

    var videoItems = videoUris.map { uris ->
            uris.map { pair ->
                VideoItem(
                    contentUri = pair.second,
                    mediaItem = MediaItem.fromUri(pair.second),
                    name = metadataReader.getMetadataFromUri(pair.second)?.fileName ?: "No name"
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        player.prepare()
    }

    fun addVideoUri(uri: Uri, name: String? = null) {
        videoUris.value = videoUris.value + Pair(name, uri)
        val defaultValue = videoUris.value.map { pair ->
            VideoItem(
                contentUri = pair.second,
                mediaItem = MediaItem.fromUri(pair.second),
                name = pair.first ?: metadataReader.getMetadataFromUri(pair.second)?.fileName ?: "No name"
            )
        }
        videoItems = videoUris.map { uris ->
            uris.map { pair ->
                VideoItem(
                    contentUri = pair.second,
                    mediaItem = MediaItem.fromUri(pair.second),
                    name = pair.first ?: metadataReader.getMetadataFromUri(pair.second)?.fileName ?: "No name"
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultValue)
        player.addMediaItem(MediaItem.fromUri(uri))
    }

    fun playVideo(uri: Uri) {
        player.setMediaItem(
            videoItems.value.find { it.contentUri == uri }?.mediaItem ?: return
        )
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}