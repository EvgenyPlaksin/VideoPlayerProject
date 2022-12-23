package com.lnight.videoplayerproject.presentation.upload_video_screen

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.lnight.videoplayerproject.R
import com.lnight.videoplayerproject.common.WorkerKeys
import com.lnight.videoplayerproject.common.size
import com.lnight.videoplayerproject.data.repository.VideoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.*
import kotlin.random.Random


@HiltWorker
class DownloadVideoWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: VideoRepository
) : CoroutineWorker(appContext = context, params = params) {

    override suspend fun doWork(): Result {
        startForegroundService()
        try {
            val emptyUriError = Data.Builder()
                .putString(WorkerKeys.ERROR_MSG, "Url cannot be empty!")
                .build()
            val videoUri =
                params.inputData.getString(WorkerKeys.VIDEO_URI) ?: return Result.failure(
                    emptyUriError
                )
            if (videoUri.isBlank()) return Result.failure(emptyUriError)

            val response = repository.downloadVideo(videoUri)

            when (response.isSuccessful) {
                true -> {
                    var uri: String
                    response.body()?.let { body ->
                        return withContext(Dispatchers.IO) {
                            try {
                                val fileName = videoUri.substring(videoUri.lastIndexOf("/") + 1)
                                val pathToSave = context.filesDir.absolutePath + fileName
                                uri = saveFile(body, pathToSave)
                                val file = File(uri)
                                uri = copyFileToDownloads(file)
                            } catch (e: IOException) {
                                val notificationManager = NotificationManagerCompat.from(context)
                                notificationManager.cancelAll()
                                return@withContext Result.failure(
                                    workDataOf(
                                        WorkerKeys.ERROR_MSG to e.localizedMessage
                                    )
                                )
                            }
                            val notificationManager = NotificationManagerCompat.from(context)
                            notificationManager.cancelAll()
                            Result.success(
                                workDataOf(
                                    WorkerKeys.LOCAL_VIDEO_URI to uri
                                )
                            )
                        }
                    }
                }
                false -> {
                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.cancelAll()
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
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()
            return Result.failure(
                workDataOf(
                    WorkerKeys.ERROR_MSG to "Network error"
                )
            )
        }
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
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
                NotificationCompat.Builder(context, "download_channel")
                    .setSmallIcon(R.drawable.ic_download)
                    .setContentText("Downloading")
                    .setContentTitle("Download in progress")
                    .setOngoing(true)
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
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            input?.close()
        }
        return ""
    }

    private fun copyFileToDownloads(downloadedFile: File): String {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, downloadedFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeTypeForFile(downloadedFile))
                put(MediaStore.MediaColumns.SIZE, downloadedFile.size)
            }
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            val authority = "${context.packageName}.provider"
            val destinyFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                downloadedFile.name
            )
            FileProvider.getUriForFile(context, authority, destinyFile)
        }?.also { downloadedUri ->
            resolver.openOutputStream(downloadedUri).use { outputStream ->
                val brr = ByteArray(1024)
                var len: Int
                val bufferedInputStream =
                    BufferedInputStream(FileInputStream(downloadedFile.absoluteFile))
                while ((bufferedInputStream.read(brr, 0, brr.size).also { len = it }) != -1) {
                    outputStream?.write(brr, 0, len)
                }
                outputStream?.flush()
                bufferedInputStream.close()
            }
        }.toString()
    }

    private fun getMimeTypeForFile(finalFile: File): String =
        DocumentFile.fromFile(finalFile).type ?: "application/octet-stream"

}