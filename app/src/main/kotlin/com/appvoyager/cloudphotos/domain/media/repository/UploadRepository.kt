package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.model.UploadResult
import com.appvoyager.cloudphotos.domain.media.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath

interface UploadRepository {

    suspend fun uploadMedia(request: UploadMediaRequest): UploadResult<CloudStoragePath>

}
