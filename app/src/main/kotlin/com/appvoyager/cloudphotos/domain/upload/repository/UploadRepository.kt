package com.appvoyager.cloudphotos.domain.upload.repository

import com.appvoyager.cloudphotos.domain.upload.model.UploadResult
import com.appvoyager.cloudphotos.domain.upload.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.upload.valueobject.StoragePath

interface UploadRepository {

    suspend fun uploadMedia(request: UploadMediaRequest): UploadResult<StoragePath>

}
