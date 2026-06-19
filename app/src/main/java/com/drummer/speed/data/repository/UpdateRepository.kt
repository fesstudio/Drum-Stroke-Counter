package com.drummer.speed.data.repository

import com.drummer.speed.domain.model.DownloadResult
import com.drummer.speed.domain.model.UpdateCheckResult
import com.drummer.speed.domain.repository.IUpdateRepository
import com.drummer.speed.util.AudioConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor() : IUpdateRepository {

    override suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val jsonContent = URL(AudioConfig.UPDATE_URL).readText()
            val gson = Gson()
            @Suppress("UNCHECKED_CAST")
            val data = gson.fromJson(jsonContent, Map::class.java) as? Map<String, Any>
            if (data != null) {
                val versionCode = (data["versionCode"] as? Double)?.toInt() ?: 0
                val downloadUrl = data["downloadUrl"] as? String ?: ""
                UpdateCheckResult.Available(versionCode = versionCode, downloadUrl = downloadUrl)
            } else {
                UpdateCheckResult.Error("Invalid response format")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateCheckResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun downloadApk(url: String, targetFilePath: String): Flow<DownloadResult> = callbackFlow {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    trySend(DownloadResult.Error("Server returned HTTP ${connection.responseCode}"))
                    close()
                    return@withContext
                }

                val fileLength = connection.contentLength
                val input = connection.inputStream
                val output = FileOutputStream(File(targetFilePath))

                val data = ByteArray(AudioConfig.DOWNLOAD_BUFFER_SIZE)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        trySend(DownloadResult.Progress(((total * 100) / fileLength).toInt()))
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                trySend(DownloadResult.Success(File(targetFilePath)))
                close()
            } catch (e: Exception) {
                trySend(DownloadResult.Error(e.message ?: "Unknown error"))
                close()
            }
        }
        awaitClose { }
    }
}
