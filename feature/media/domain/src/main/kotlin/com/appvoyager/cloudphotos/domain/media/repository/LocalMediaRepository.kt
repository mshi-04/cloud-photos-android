package com.appvoyager.cloudphotos.domain.media.repository

import com.appvoyager.cloudphotos.domain.media.model.Media
import kotlinx.coroutines.flow.Flow

interface LocalMediaRepository {

    fun getMediaListFlow(): Flow<List<Media>>

    suspend fun getMediaList(): List<Media>

}
