package com.lnight.videoplayerproject.data.repository

import okhttp3.ResponseBody

interface VideoRepository {

   suspend fun downloadVideo(videoUri: String): Result<ResponseBody?>

}