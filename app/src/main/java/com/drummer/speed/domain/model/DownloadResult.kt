package com.drummer.speed.domain.model

import java.io.File

sealed class DownloadResult {
    data class Progress(val percentage: Int) : DownloadResult()
    data class Success(val file: File) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}
