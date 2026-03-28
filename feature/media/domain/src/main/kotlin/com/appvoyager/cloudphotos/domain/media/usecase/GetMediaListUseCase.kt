package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.repository.LocalMediaRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetMediaListUseCase @Inject constructor(private val localMediaRepository: LocalMediaRepository) {

    operator fun invoke(): Flow<List<Media>> = localMediaRepository.getMediaListFlow()
}
