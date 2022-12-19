package com.lnight.videoplayerproject.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface VideoApi {

    @Streaming
    @GET
    suspend fun downloadVideo(@Url videoUri: String): Response<ResponseBody>

}