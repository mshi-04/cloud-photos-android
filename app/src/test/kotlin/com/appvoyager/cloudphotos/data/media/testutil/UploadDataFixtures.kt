package com.appvoyager.cloudphotos.data.media.testutil

import com.appvoyager.cloudphotos.domain.media.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.ContentType
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl

fun uploadMediaRequestFixture(
    localUri: String = "content://media/external/images/media/1",
    contentType: String = "image/jpeg"
): UploadMediaRequest =
    UploadMediaRequest(
        localUri = MediaUrl.of(localUri),
        contentType = ContentType.of(contentType)
    )

fun cloudStoragePathFixture(
    path: String = "media/identity123/image.jpg"
): CloudStoragePath = CloudStoragePath.of(path)
