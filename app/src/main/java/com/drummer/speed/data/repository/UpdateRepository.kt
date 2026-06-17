package com.drummer.speed.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor() {
    
    sealed class DownloadStatus {
        data class Progress(val percentage: Int) : DownloadStatus()
        data class Success(val file: File) : DownloadStatus()
        data class Error(val message: String) : DownloadStatus()
    }

    suspend fun checkForUpdates(): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val versionJsonUrl = "https://raw.githubusercontent.com/fesstudio/Drum-Stroke-Counter/master/version.json"
            val jsonContent = URL(versionJsonUrl).readText()
            val gson = Gson()
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(jsonContent, Map::class.java) as? Map<String, Any>
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun downloadApk(url: String, targetFile: File): Flow<DownloadStatus> = callbackFlow {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    trySend(DownloadStatus.Error("Server returned HTTP ${connection.responseCode}"))
                    close()
                    return@withContext
                }

                val fileLength = connection.contentLength
                val input = connection.inputStream
                val output = FileOutputStream(targetFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        trySend(DownloadStatus.Progress(((total * 100) / fileLength).toInt()))
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                trySend(DownloadStatus.Success(targetFile))
                close()
            } catch (e: Exception) {
                trySend(DownloadStatus.Error(e.message ?: "Unknown error"))
                close()
            }
        }
        awaitClose { }
    }
}
