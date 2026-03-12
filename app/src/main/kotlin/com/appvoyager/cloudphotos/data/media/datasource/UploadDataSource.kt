package com.appvoyager.cloudphotos.data.media.datasource

import com.appvoyager.cloudphotos.domain.media.model.UploadResult
import com.appvoyager.cloudphotos.domain.media.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath

interface UploadDataSource {

    suspend fun uploadMedia(request: UploadMediaRequest): UploadResult<CloudStoragePath>

}
