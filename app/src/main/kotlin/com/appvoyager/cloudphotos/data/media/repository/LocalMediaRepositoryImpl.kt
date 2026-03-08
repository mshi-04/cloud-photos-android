package com.appvoyager.cloudphotos.data.media.repository

import com.appvoyager.cloudphotos.data.media.datasource.LocalMediaDataSource
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.repository.LocalMediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LocalMediaRepositoryImpl @Inject constructor(
    private val localMediaDataSource: LocalMediaDataSource
) : LocalMediaRepository {

    override fun getMediaList(): Flow<Result<List<Media>>> = flow {
        val result = runCatching {
            localMediaDataSource.getLocalMediaList()
        }
        emit(result)
    }

}
