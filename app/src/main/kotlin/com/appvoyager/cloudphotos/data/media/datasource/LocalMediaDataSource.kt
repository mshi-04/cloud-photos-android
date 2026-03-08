package com.appvoyager.cloudphotos.data.media.datasource

import com.appvoyager.cloudphotos.domain.media.model.Media

interface LocalMediaDataSource {

    suspend fun getLocalMediaList(): List<Media>

}
