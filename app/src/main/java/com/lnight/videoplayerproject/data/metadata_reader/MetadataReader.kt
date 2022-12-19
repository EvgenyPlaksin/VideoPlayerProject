package com.lnight.videoplayerproject.data.metadata_reader

import android.net.Uri

interface MetadataReader {
    fun getMetadataFromUri(contentUri: Uri): Metadata?
}