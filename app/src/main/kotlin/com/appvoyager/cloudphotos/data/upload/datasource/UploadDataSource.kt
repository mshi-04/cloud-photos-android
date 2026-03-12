package com.appvoyager.cloudphotos.data.upload.datasource

import com.appvoyager.cloudphotos.domain.upload.model.UploadResult
import com.appvoyager.cloudphotos.domain.upload.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.upload.valueobject.StoragePath

interface UploadDataSource {

    suspend fun uploadMedia(request: UploadMediaRequest): UploadResult<StoragePath>

}
