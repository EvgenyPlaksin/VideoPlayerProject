package com.lnight.videoplayerproject.presentation

import android.app.Application
import android.net.Uri
import android.provider.MediaStore

data class Metadata(
    val fileName: String
)

interface MetadataReader {
    fun getMetadataFromUri(contentUri: Uri): Metadata?
}

class MetadataReaderImpl(
    private val app: Application
) : MetadataReader {

    override fun getMetadataFromUri(contentUri: Uri): Metadata? {
        if(contentUri.scheme != "content") {
            return null
        }
        val fileName = app.contentResolver
            .query(
                contentUri,
                arrayOf(MediaStore.Video.VideoColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            ?.use { cursor ->
                val index = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(index)
            }
        return fileName?.let { fullFileName ->
            Metadata(
                fileName = Uri.parse(fullFileName).lastPathSegment ?: return null
            )
        }
    }
}