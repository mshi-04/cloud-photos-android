package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.repository.LocalMediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMediaListUseCase @Inject constructor(
    private val localMediaRepository: LocalMediaRepository
) {
    operator fun invoke(): Flow<List<Media>> = localMediaRepository.getMediaList()
}
