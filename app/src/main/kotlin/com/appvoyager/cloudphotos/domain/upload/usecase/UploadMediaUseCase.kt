package com.appvoyager.cloudphotos.domain.upload.usecase

import com.appvoyager.cloudphotos.domain.upload.model.UploadResult
import com.appvoyager.cloudphotos.domain.upload.repository.UploadRepository
import com.appvoyager.cloudphotos.domain.upload.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.upload.valueobject.StoragePath
import javax.inject.Inject

class UploadMediaUseCase @Inject constructor(
    private val repository: UploadRepository
) {

    suspend operator fun invoke(request: UploadMediaRequest): UploadResult<StoragePath> =
        repository.uploadMedia(request)

}
