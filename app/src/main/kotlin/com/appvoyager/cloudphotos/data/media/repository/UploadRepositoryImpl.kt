package com.appvoyager.cloudphotos.data.media.repository

import com.appvoyager.cloudphotos.data.media.datasource.UploadDataSource
import com.appvoyager.cloudphotos.domain.media.model.UploadResult
import com.appvoyager.cloudphotos.domain.media.repository.UploadRepository
import com.appvoyager.cloudphotos.domain.media.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import javax.inject.Inject

class UploadRepositoryImpl @Inject constructor(
    private val dataSource: UploadDataSource
) : UploadRepository {

    override suspend fun uploadMedia(request: UploadMediaRequest): UploadResult<CloudStoragePath> =
        dataSource.uploadMedia(request)

}