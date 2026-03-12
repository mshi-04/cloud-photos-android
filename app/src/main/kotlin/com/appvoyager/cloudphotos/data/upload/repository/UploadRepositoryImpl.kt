package com.appvoyager.cloudphotos.data.upload.repository

import com.appvoyager.cloudphotos.data.upload.datasource.UploadDataSource
import com.appvoyager.cloudphotos.domain.upload.model.UploadResult
import com.appvoyager.cloudphotos.domain.upload.repository.UploadRepository
import com.appvoyager.cloudphotos.domain.upload.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.upload.valueobject.StoragePath
import javax.inject.Inject

class UploadRepositoryImpl @Inject constructor(
    private val dataSource: UploadDataSource
) : UploadRepository {

    override suspend fun uploadMedia(request: UploadMediaRequest): UploadResult<StoragePath> =
        dataSource.uploadMedia(request)

}
