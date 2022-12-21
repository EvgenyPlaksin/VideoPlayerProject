package com.lnight.videoplayerproject.data.repository

import com.lnight.videoplayerproject.data.remote.VideoApi
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val api: VideoApi
): VideoRepository {

    override suspend fun downloadVideo(videoUri: String): Response<ResponseBody> {
        return api.downloadVideo(videoUri)
    }
}