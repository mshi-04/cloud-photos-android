package com.appvoyager.cloudphotos.data.media.worker

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContentTypeResolverImpl @Inject constructor(@param:ApplicationContext private val context: Context) :
    ContentTypeResolver {

    override fun resolve(mediaId: MediaId): String? {
        val uri = buildUri(mediaId) ?: return null
        return context.contentResolver.getType(uri)
    }

    override fun resolveUri(mediaId: MediaId): MediaUrl? {
        val uri = buildUri(mediaId) ?: return null
        return MediaUrl.of(uri.toString())
    }

    private fun buildUri(mediaId: MediaId): Uri? {
        val lastUnderscore = mediaId.value.lastIndexOf('_')
        if (lastUnderscore < 0) return null
        val volumeName = mediaId.value.substring(0, lastUnderscore)
        val id = mediaId.value.substring(lastUnderscore + 1).toLongOrNull() ?: return null
        return ContentUris.withAppendedId(
            MediaStore.Files.getContentUri(volumeName),
            id
        )
    }
}
