package com.lnight.videoplayerproject.data.repository

import okhttp3.ResponseBody
import retrofit2.Response

interface VideoRepository {

   suspend fun downloadVideo(videoUri: String): Response<ResponseBody>

}