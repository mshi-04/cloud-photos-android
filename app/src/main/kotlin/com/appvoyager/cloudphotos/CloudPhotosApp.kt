package com.appvoyager.cloudphotos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.amplifyframework.AmplifyException
import com.amplifyframework.api.aws.AWSApiPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.appvoyager.cloudphotos.data.media.worker.UploadNotificationHelper
import com.appvoyager.cloudphotos.fcm.CloudPhotosFirebaseMessagingService
import com.appvoyager.cloudphotos.fcm.FcmTokenRegistrar
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CloudPhotosApp :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var uploadNotificationHelper: UploadNotificationHelper

    @Inject
    lateinit var fcmTokenRegistrar: FcmTokenRegistrar

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.addPlugin(AWSS3StoragePlugin())
            Amplify.addPlugin(AWSApiPlugin())
            Amplify.configure(applicationContext)
        } catch (error: AmplifyException) {
            throw RuntimeException(
                "Amplify initialization failed. Check amplifyconfiguration.json.",
                error
            )
        }
        uploadNotificationHelper.createChannel()
        createUploadCompleteNotificationChannel()
        fcmTokenRegistrar.register()
    }

    private fun createUploadCompleteNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CloudPhotosFirebaseMessagingService.CHANNEL_ID,
            getString(R.string.notification_channel_upload_complete),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = getString(R.string.notification_channel_upload_complete_description)
        manager.createNotificationChannel(channel)
    }
}
