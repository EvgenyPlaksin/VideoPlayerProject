package com.lnight.videoplayerproject.data.repository

import com.lnight.videoplayerproject.data.remote.VideoApi
import okhttp3.ResponseBody
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val api: VideoApi
): VideoRepository {

    override suspend fun downloadVideo(videoUri: String): Result<ResponseBody?> {
        return try {
            val data = api.downloadVideo(videoUri)
            Result.success(data.body())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}