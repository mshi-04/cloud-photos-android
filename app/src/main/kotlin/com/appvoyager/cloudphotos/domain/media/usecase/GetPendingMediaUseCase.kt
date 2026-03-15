package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.repository.LocalMediaRepository
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import javax.inject.Inject

class GetPendingMediaUseCase @Inject constructor(
    private val localMediaRepository: LocalMediaRepository,
    private val localUploadRecordsRepository: LocalUploadRecordsRepository
) {

    suspend operator fun invoke(): List<Media> {
        val allLocalMedia = localMediaRepository.getMediaList()
        val mediaIds = allLocalMedia.map { it.id }
        val uploadRecords = mediaIds.chunked(900).flatMap { chunkedIds ->
            localUploadRecordsRepository.getUploadRecords(chunkedIds)
        }

        val uploadedMediaIds = uploadRecords.map { it.mediaId }.toSet()
        return allLocalMedia.filter { it.id !in uploadedMediaIds }
    }

}
