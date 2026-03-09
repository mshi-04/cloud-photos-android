package com.appvoyager.cloudphotos.data.media.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalMediaDataSourceImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : LocalMediaDataSource {

    override suspend fun getLocalMediaList(): List<Media> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<Media>()
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DURATION
        )

        val pathFilter = TARGET_PATHS.joinToString(" OR ") {
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        }
        val selection =
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)" +
                    " AND ($pathFilter)"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            *TARGET_PATHS
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val volumeNames = MediaStore.getExternalVolumeNames(context)
        for (volumeName in volumeNames) {
            contentResolver.query(
                MediaStore.Files.getContentUri(volumeName),
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val typeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val dateColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val idLong = cursor.getLong(idColumn)
                    val mediaTypeInt = cursor.getInt(typeColumn)
                    val dateAddedSeconds = cursor.getLong(dateColumn)

                    val type = if (mediaTypeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        MediaType.IMAGE
                    } else {
                        MediaType.VIDEO
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Files.getContentUri(volumeName),
                        idLong
                    )

                    mediaList.add(
                        Media(
                            id = MediaId.of("${volumeName}_${idLong}"),
                            url = MediaUrl.of(contentUri.toString()),
                            type = type,
                            thumbnailUrl = null,
                            createdAt = MediaCreatedAt.of(dateAddedSeconds * 1000L)
                        )
                    )
                }
            }
        }
        mediaList.sortByDescending { it.createdAt.value }
        mediaList
    }

    companion object {
        private val TARGET_PATHS = arrayOf("DCIM/%", "Pictures/%", "Download/%")
    }

}