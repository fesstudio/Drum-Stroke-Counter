package com.drummer.speed.domain.usecase

import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.domain.repository.IHistoryRepository
import javax.inject.Inject

class SaveResultUseCase @Inject constructor(
    private val repository: IHistoryRepository
) {
    suspend operator fun invoke(result: SessionResult) = repository.insertResult(result)
}
