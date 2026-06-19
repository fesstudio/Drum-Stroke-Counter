package com.drummer.speed.domain.usecase

import com.drummer.speed.domain.model.DownloadResult
import com.drummer.speed.domain.repository.IUpdateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DownloadUpdateUseCase @Inject constructor(
    private val repository: IUpdateRepository
) {
    operator fun invoke(url: String, targetFilePath: String): Flow<DownloadResult> {
        return repository.downloadApk(url, targetFilePath)
    }
}
