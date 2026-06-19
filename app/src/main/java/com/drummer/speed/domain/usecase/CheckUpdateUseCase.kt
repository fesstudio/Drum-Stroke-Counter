package com.drummer.speed.domain.usecase

import com.drummer.speed.domain.model.UpdateCheckResult
import com.drummer.speed.domain.repository.IUpdateRepository
import javax.inject.Inject

class CheckUpdateUseCase @Inject constructor(
    private val repository: IUpdateRepository
) {
    suspend operator fun invoke(currentVersionCode: Int): UpdateCheckResult {
        val result = repository.checkForUpdates()
        return when (result) {
            is UpdateCheckResult.Available -> {
                if (result.versionCode > currentVersionCode) result
                else UpdateCheckResult.UpToDate
            }
            is UpdateCheckResult.UpToDate -> result
            is UpdateCheckResult.Error -> result
        }
    }
}
