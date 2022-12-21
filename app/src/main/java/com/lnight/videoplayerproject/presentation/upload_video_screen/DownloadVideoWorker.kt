package com.lnight.videoplayerproject.presentation.upload_video_screen

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lnight.videoplayerproject.R
import com.lnight.videoplayerproject.common.WorkerKeys
import com.lnight.videoplayerproject.data.repository.VideoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.random.Random

@HiltWorker
class DownloadVideoWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: VideoRepository
): CoroutineWorker(appContext = context, params = params) {

    override suspend fun doWork(): Result {
        startForegroundService()
        try {
            val videoUri =
                params.inputData.getString(WorkerKeys.VIDEO_URI) ?: return Result.failure()
            val response = repository.downloadVideo(videoUri)

            when (response.isSuccessful) {
                true -> {
                    var uri: String
                    response.body()?.let { body ->
                        return withContext(Dispatchers.IO) {
                            try {
                                val fileName = videoUri.substring(videoUri.lastIndexOf("/")+1)
                                val pathToSave = context.filesDir.absolutePath+fileName
                                uri = saveFile(body, pathToSave)
                            } catch (e: IOException) {
                                return@withContext Result.failure(
                                    workDataOf(
                                        WorkerKeys.ERROR_MSG to e.localizedMessage
                                    )
                                )
                            }
                            Result.success(
                                workDataOf(
                                    WorkerKeys.LOCAL_VIDEO_URI to uri
                                )
                            )
                        }
                    }
                }
                false -> {
                    if (response.code().toString().startsWith("5")) {
                        return Result.retry()
                    }
                    return Result.failure(
                        workDataOf(
                            WorkerKeys.ERROR_MSG to "Network error"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return Result.failure(
                workDataOf(
                    WorkerKeys.ERROR_MSG to "Network error"
                )
            )
        }
        return Result.failure(
            workDataOf(
                WorkerKeys.ERROR_MSG to "Unknown error"
            )
        )
    }

    private suspend fun startForegroundService() {
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                NotificationCompat.Builder(context,"download_channel")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentText("Downloading")
                    .setContentTitle("Download in progress")
                    .build()
            )
        )
    }

    private fun saveFile(body: ResponseBody, pathWhereYouWantToSaveFile: String): String {
        var input: InputStream? = null
        try {
            input = body.byteStream()
            val fos = FileOutputStream(pathWhereYouWantToSaveFile)
            fos.use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
            return pathWhereYouWantToSaveFile
        }catch (e:Exception){
            Log.e("saveFile", e.toString() )
        }
        finally {
            input?.close()
        }
        return ""
    }

}