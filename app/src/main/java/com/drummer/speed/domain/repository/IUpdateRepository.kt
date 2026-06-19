package com.drummer.speed.domain.repository

import com.drummer.speed.domain.model.DownloadResult
import com.drummer.speed.domain.model.UpdateCheckResult
import kotlinx.coroutines.flow.Flow

interface IUpdateRepository {
    suspend fun checkForUpdates(): UpdateCheckResult
    fun downloadApk(url: String, targetFilePath: String): Flow<DownloadResult>
}
