package com.drummer.speed.domain.usecase

import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.domain.repository.IHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val repository: IHistoryRepository
) {
    operator fun invoke(): Flow<List<SessionResult>> = repository.getAllHistory()
}
