package com.drummer.speed.domain.model

sealed class UpdateCheckResult {
    data class Available(
        val versionCode: Int,
        val downloadUrl: String
    ) : UpdateCheckResult()

    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
