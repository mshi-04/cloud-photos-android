package com.appvoyager.cloudphotos.data.media.worker

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContentTypeResolverImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ContentTypeResolver {

    override fun resolve(mediaId: MediaId): String? {
        val lastUnderscore = mediaId.value.lastIndexOf('_')
        if (lastUnderscore < 0) return null
        val volumeName = mediaId.value.substring(0, lastUnderscore)
        val id = mediaId.value.substring(lastUnderscore + 1).toLongOrNull() ?: return null
        val uri = ContentUris.withAppendedId(
            MediaStore.Files.getContentUri(volumeName),
            id
        )
        return context.contentResolver.getType(uri)
    }
}
