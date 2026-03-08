package com.appvoyager.cloudphotos.domain.media.datasource

import com.appvoyager.cloudphotos.domain.media.model.Media

interface LocalMediaDataSource {

    suspend fun getLocalMediaList(): List<Media>

}
