package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.model.Media
import kotlinx.coroutines.flow.Flow

interface LocalMediaRepository {

    fun getMediaList(): Flow<Result<List<Media>>>

}
