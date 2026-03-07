package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.model.CloudMedia
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMediaList(): Flow<List<CloudMedia>>
}
