package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.UploadResult
import com.appvoyager.cloudphotos.domain.media.repository.UploadRepository
import com.appvoyager.cloudphotos.domain.media.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import javax.inject.Inject

class UploadMediaUseCase @Inject constructor(
    private val repository: UploadRepository
) {

    suspend operator fun invoke(request: UploadMediaRequest): UploadResult<CloudStoragePath> =
        repository.uploadMedia(request)

}