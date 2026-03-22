package com.appvoyager.cloudphotos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.amplifyframework.AmplifyException
import com.amplifyframework.api.aws.AWSApiPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.appvoyager.cloudphotos.data.media.worker.UploadNotificationHelper
import com.appvoyager.cloudphotos.fcm.CloudPhotosFirebaseMessagingService
import com.appvoyager.cloudphotos.fcm.RegisterDeviceTokenWorker
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CloudPhotosApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var uploadNotificationHelper: UploadNotificationHelper

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
        registerFcmToken()
    }

    private fun createUploadCompleteNotificationChannel() {
        val channel = NotificationChannel(
            CloudPhotosFirebaseMessagingService.CHANNEL_ID,
            getString(R.string.notification_channel_upload_complete),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            scheduleDeviceTokenRegistration(token)
        }
    }

    internal fun scheduleDeviceTokenRegistration(token: String) {
        val request = OneTimeWorkRequestBuilder<RegisterDeviceTokenWorker>()
            .setInputData(workDataOf(RegisterDeviceTokenWorker.KEY_TOKEN to token))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                RegisterDeviceTokenWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

}
